package fairshare.infrastructure.persistence

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupStoreUnavailable
import fairshare.domain.group.Group
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.GroupSettlementCurrencyChanged
import fairshare.domain.group.SettlementCurrency
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SqliteGroupStoreTest :
    BehaviorSpec({
        given("two instances sharing a durable Group store") {
            then("only one of two concurrent batches commits at the same revision, without interleaving") {
                val url = "jdbc:sqlite:${tempdir().resolve("groups.db")}"
                val first = store(url)
                val second = store(url)
                val other = GroupId("unrelated")
                val opened = first.append(trip, null, opening).committed()
                val otherOpened = second.append(other, null, opening).committed()
                val batchA = changes("EUR", "JPY")
                val batchB = changes("KWD", "USD")

                val barrier = CyclicBarrier(2)
                val outcomes = Executors.newFixedThreadPool(2).use { workers ->
                    listOf(first to batchA, second to batchB).map { (writer, batch) ->
                        workers.submit<Pair<GroupEventBatch, GroupAppend>> {
                            val expected = checkNotNull(writer.load(trip)).revision
                            expected shouldBe opened.revision
                            barrier.await(5, TimeUnit.SECONDS)
                            batch to writer.append(trip, expected, batch)
                        }
                    }.map { it.get(10, TimeUnit.SECONDS) }
                }
                val (events, committed) = outcomes.single { it.second is GroupAppend.Committed }
                outcomes.single { it.first !== events }.second shouldBe GroupAppend.RevisionMismatch
                committed.committed().revision shouldNotBe opened.revision
                val loaded = checkNotNull(first.load(trip))
                loaded.revision shouldBe committed.committed().revision
                loaded.events shouldBe opening.events + events.events
                second.load(trip) shouldBe loaded
                first.load(other)?.revision shouldBe otherOpened.revision
                first.append(other, loaded.revision, batchA) shouldBe GroupAppend.RevisionMismatch
                first.load(other)?.events shouldBe opening.events
            }

            then("a late insert failure rolls back the entire batch, including an opening event") {
                val url = "jdbc:sqlite:${tempdir().resolve("rollback.db")}"
                val writer = SqliteGroupStore(SqliteGroupStoreSettings(jdbcUrl = url, busyTimeoutMillis = 0))
                val reader = store(url)
                val revision = writer.append(trip, null, opening).committed().revision
                val absent = GroupId("absent")
                val before = reader.load(trip)
                database(url) { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute(
                            """
                            CREATE TRIGGER refuse_usd BEFORE INSERT ON group_events
                            WHEN NEW.currency = 'USD'
                            BEGIN SELECT RAISE(IGNORE); END
                            """.trimIndent(),
                        )
                    }
                }
                shouldThrow<GroupStoreUnavailable> { writer.append(trip, revision, changes("EUR", "USD")) }
                reader.load(trip) shouldBe before
                val failsInSecondRow = GroupEventBatch(opening.events + changes("USD").events)
                shouldThrow<GroupStoreUnavailable> { writer.append(absent, null, failsInSecondRow) }
                reader.load(absent) shouldBe null
                writer.append(trip, revision, changes("JPY")).committed()
                reader.load(trip)?.events shouldBe opening.events + changes("JPY").events
            }

            then("a failure at commit is an unknown outcome and releases the write transaction") {
                val rawUrl = "jdbc:sqlite:${tempdir().resolve("commit-failure.db").toURI()}"
                val writer = SqliteGroupStore(
                    SqliteGroupStoreSettings(jdbcUrl = "$rawUrl?foreign_keys=on", busyTimeoutMillis = 0),
                )
                val before = writer.append(trip, null, opening).committed().revision
                database(rawUrl) { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("CREATE TABLE parent (id INTEGER PRIMARY KEY)")
                        statement.execute(
                            """
                            CREATE TABLE child (
                                parent_id INTEGER REFERENCES parent(id) DEFERRABLE INITIALLY DEFERRED
                            )
                            """.trimIndent(),
                        )
                        statement.execute(
                            """
                            CREATE TRIGGER fail_at_commit AFTER INSERT ON group_events
                            WHEN NEW.currency = 'USD'
                            BEGIN INSERT INTO child VALUES (1); END
                            """.trimIndent(),
                        )
                    }
                }
                shouldThrow<GroupAppendOutcomeUnknown> { writer.append(trip, before, changes("EUR", "USD")) }
                writer.load(trip)?.revision shouldBe before
                writer.load(trip)?.events shouldBe opening.events
                writer.append(trip, before, changes("EUR")).committed()
            }

            then("a confirmed read-only write failure leaves the previously committed currency and revision intact") {
                val database = tempdir().resolve("groups.db").toURI()
                val writable = store("jdbc:sqlite:$database")
                val created = writable.append(trip, null, opening).committed()
                val readOnly = store("jdbc:sqlite:$database?mode=ro")
                readOnly.load(trip)?.revision shouldBe created.revision
                shouldThrow<GroupStoreUnavailable> { readOnly.append(trip, created.revision, changes("EUR")) }
                val persisted = checkNotNull(writable.load(trip))
                persisted.revision shouldBe created.revision
                Group.replay(persisted.events).settlementCurrency.code shouldBe "PLN"
            }

            then("creation can only start an empty stream and a configured Group cannot be re-created") {
                val url = "jdbc:sqlite:${tempdir().resolve("groups.db")}"
                val writer = store(url)
                val batch = GroupEventBatch(opening.events + changes("EUR", "JPY").events)
                val created = writer.append(trip, null, batch).committed()
                writer.load(trip)?.events shouldBe batch.events
                database(url) { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT kind, display_name, format_version FROM group_events ORDER BY position",
                        )
                            .use { rows ->
                                buildList {
                                    while (rows.next()) {
                                        add(
                                            Triple(rows.getString(1), rows.getString(2), rows.getInt(3)),
                                        )
                                    }
                                } shouldBe listOf(
                                    Triple("group-created", "Trip", 1),
                                    Triple("settlement-currency-changed", null, 1),
                                    Triple("settlement-currency-changed", null, 1),
                                )
                            }
                    }
                }
                writer.append(trip, null, opening) shouldBe GroupAppend.RevisionMismatch
                writer.load(trip)?.revision shouldBe created.revision
                shouldThrow<IllegalArgumentException> { writer.append(trip, created.revision, opening) }

                val absent = GroupId("absent")
                writer.append(absent, created.revision, changes("EUR")) shouldBe GroupAppend.RevisionMismatch
                writer.load(absent) shouldBe null
                shouldThrow<IllegalArgumentException> { writer.append(absent, null, changes("EUR")) }
            }
        }
    })

private val trip = GroupId("trip")
private val opening = GroupEventBatch(
    listOf(Group.create(checkNotNull(GroupName.parse("Trip")), checkNotNull(SettlementCurrency.parse("PLN")))),
)

private fun changes(vararg codes: String): GroupEventBatch =
    GroupEventBatch(codes.map { GroupSettlementCurrencyChanged(checkNotNull(SettlementCurrency.parse(it))) })

private fun GroupAppend.committed(): GroupAppend.Committed = this as GroupAppend.Committed

internal fun store(url: String): SqliteGroupStore = SqliteGroupStore(SqliteGroupStoreSettings(jdbcUrl = url))

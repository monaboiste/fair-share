package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.SQLException

class SqliteEventStoreRollbackTest :
    ExpectSpec({
        val streamId = StreamId("original-stream")

        expect("a globally duplicate event ID rolls back an earlier insert and the new stream header") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val otherId = StreamId("other-stream")
            val other = pending(TestCreated("other"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))

            val failure = shouldThrow<SQLException> {
                database.store.append(otherId, StreamRevision.INITIAL, listOf(other, created))
            }
            failure.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE

            database.store.load(otherId) shouldBe null
            database.store.load(streamId).shouldNotBeNull().events shouldBe listOf(created.envelope(streamId, 1, 1))
            database.store.append(otherId, StreamRevision.INITIAL, listOf(other)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            database.store.load(otherId).shouldNotBeNull().events shouldBe listOf(other.envelope(otherId, 1, 1))
        }

        listOf("aborts" to "RAISE(ABORT, 'rejected event')", "ignores" to "RAISE(IGNORE)").forEach { (action, raise) ->
            expect("the whole append rolls back when a trigger $action its second insert") {
                val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
                val created = pending(TestCreated("original"))
                val renamed = pending(TestRenamed("first of batch"))
                val rejected = pending(TestRenamed("second of batch"))
                database.store.append(streamId, StreamRevision.INITIAL, listOf(created))
                database.executeSql(
                    """
                    CREATE TRIGGER reject_event BEFORE INSERT ON event
                    WHEN NEW.event_id = '${rejected.eventId.value}'
                    BEGIN SELECT $raise; END
                    """.trimIndent(),
                )

                if (action == "aborts") {
                    val failure = shouldThrow<SQLException> {
                        database.store.append(streamId, StreamRevision(1), listOf(renamed, rejected))
                    }
                    failure.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                        SQLiteErrorCode.SQLITE_CONSTRAINT_TRIGGER
                } else {
                    shouldThrow<IllegalStateException> {
                        database.store.append(streamId, StreamRevision(1), listOf(renamed, rejected))
                    }
                }

                database.store.load(streamId).shouldNotBeNull().apply {
                    revision shouldBe StreamRevision(1)
                    events shouldBe listOf(created.envelope(streamId, 1, 1))
                }
                database.executeSql("DROP TRIGGER reject_event")
                database.store.append(streamId, StreamRevision(1), listOf(renamed, rejected)) shouldBe
                    AppendResult.Committed(StreamRevision(2))
                database.store.load(streamId).shouldNotBeNull().events shouldBe listOf(
                    created.envelope(streamId, 1, 1),
                    renamed.envelope(streamId, 2, 2),
                    rejected.envelope(streamId, 3, 2),
                )
            }
        }

        expect("a rollback-journal reader preventing commit produces an unknown commit outcome") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"), busyTimeoutMillis = 0)
            database.executeSql("PRAGMA journal_mode = DELETE")
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("attempted under a read lock"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))

            database.dataSource.connection.use { reader ->
                reader.autoCommit = false
                reader.createStatement().use { statement ->
                    statement.executeQuery("SELECT revision FROM event_stream").use { rows ->
                        rows.next() shouldBe true
                        rows.getLong(1) shouldBe 1L
                    }
                }

                val failure = shouldThrow<UnknownEventCommitException> {
                    database.store.append(streamId, StreamRevision(1), listOf(renamed))
                }
                failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe SQLiteErrorCode.SQLITE_BUSY
            }

            database.store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
            database.store.append(streamId, StreamRevision(1), listOf(renamed)) shouldBe
                AppendResult.Committed(StreamRevision(2))
        }
    })

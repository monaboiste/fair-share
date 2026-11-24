package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupStoreUnavailable
import fairshare.domain.group.GroupId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class SqliteGroupWriteFailureTest :
    BehaviorSpec({
        given("SQLite failures at different stages of a conditional write") {
            then("a refused insert is not mistaken for a stale revision and a failed batch leaves no rows behind") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val writer = db.eventStore()
                val reader = db.eventStore()
                val revision = writer.append(trip, null, opening).committedRevision()
                val before = reader.load(trip)
                val absent = GroupId("absent")
                db.refuseUsd()

                val refused = shouldThrow<GroupStoreUnavailable> { writer.append(trip, revision, changes("USD")) }
                refused.cause?.message shouldContain "Group ${trip.value} append from ${revision.value}"
                shouldThrow<GroupStoreUnavailable> { writer.append(trip, revision, changes("EUR", "USD")) }
                reader.load(trip) shouldBe before
                val attemptedOpening = GroupEventBatch(opening.events + changes("USD").events)
                shouldThrow<GroupStoreUnavailable> { writer.append(absent, null, attemptedOpening) }
                reader.load(absent) shouldBe null
                val committed = writer.append(trip, revision, changes("JPY")).committedRevision()
                reader.load(trip)?.revision shouldBe committed
                reader.load(trip)?.events?.map { it.event } shouldBe opening.events + changes("JPY").events
            }

            then("rollback and close errors remain suppressed on the confirmed write failure") {
                val db = SqliteTestFixture(tempdir()).apply {
                    initialize()
                    refuseUsd()
                }
                val before = db.eventStore().append(trip, null, opening).committedRevision()
                val rollbackError = SQLException("Rollback connection failure")
                val closeError = SQLException("Close connection failure")
                val writer = SqliteGroupEventStore(
                    db.dataSource().observeConnection {
                        when (it) {
                            "rollback" -> throw rollbackError
                            "close" -> throw closeError
                        }
                    },
                    fixedClock,
                )
                val failure = shouldThrow<GroupStoreUnavailable> { writer.append(trip, before, changes("EUR", "USD")) }
                checkNotNull(failure.cause?.cause).suppressed.toList() shouldBe listOf(rollbackError, closeError)
                db.eventStore().load(trip)?.revision shouldBe before
            }

            then("a real failure inside commit is unknown and the connection releases its transaction") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val writer = db.eventStore()
                val revision = writer.append(trip, null, opening).committedRevision()
                val before = writer.load(trip)
                db.execute(
                    "CREATE TABLE parent (id INTEGER PRIMARY KEY)",
                    "CREATE TABLE child (id INTEGER REFERENCES parent(id) DEFERRABLE INITIALLY DEFERRED)",
                    """
                CREATE TRIGGER fail_at_commit AFTER INSERT ON group_events
                WHEN NEW.currency = 'USD'
                BEGIN INSERT INTO child VALUES (1); END
                    """.trimIndent(),
                )

                shouldThrow<GroupAppendOutcomeUnknown> { writer.append(trip, revision, changes("EUR", "USD")) }
                writer.load(trip) shouldBe before
                writer.append(trip, revision, changes("EUR")).committedRevision()
            }

            then("a lost acknowledgement leaves a committed write unknown and is not retried") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val reader = db.eventStore()
                val before = reader.append(trip, null, opening).committedRevision()
                val lostAcknowledgement = SQLException("Lost commit acknowledgement")
                var commits = 0
                val writer = SqliteGroupEventStore(
                    db.dataSource().observeConnection {
                        if (it == "commit") {
                            commits++
                            throw lostAcknowledgement
                        }
                    },
                    fixedClock,
                )
                val failure = shouldThrow<GroupAppendOutcomeUnknown> {
                    writer.append(trip, before, changes("EUR", "JPY"))
                }
                failure.cause?.cause shouldBe lostAcknowledgement
                commits shouldBe 1
                reader.load(trip)?.events?.map { it.event } shouldBe opening.events + changes("EUR", "JPY").events
            }

            then("a stale append releases the write transaction with explicit rollback, not commit") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                db.eventStore().append(trip, null, opening).committedRevision()
                val calls = mutableListOf<String>()
                val writer = SqliteGroupEventStore(db.dataSource().observeConnection { calls += it }, fixedClock)

                writer.append(trip, null, opening) shouldBe GroupAppend.RevisionMismatch
                calls.filter { it in setOf("commit", "rollback", "close") } shouldBe listOf("rollback", "close")
            }

            then("a read with a connection that fails to close reports a storage failure") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                db.eventStore().append(trip, null, opening).committedRevision()
                val closeError = SQLException("Close connection failure")
                val dataSource = db.dataSource().observeConnection { if (it == "close") throw closeError }
                val reader = SqliteGroupViewRepository(dataSource)

                shouldThrow<GroupStoreUnavailable> { reader.findById(trip) }.cause shouldBe closeError
            }

            then("a successful commit stays successful when closing the connection fails, without rollback") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                var rollbacks = 0
                val writer = SqliteGroupEventStore(
                    db.dataSource().observeConnection {
                        when (it) {
                            "rollback" -> rollbacks++
                            "close" -> throw SQLException("Close connection failure")
                        }
                    },
                    fixedClock,
                )
                val revision = writer.append(trip, null, opening).committedRevision()
                db.eventStore().load(trip)?.revision shouldBe revision
                rollbacks shouldBe 0
            }

            then("a read-only write failure leaves previous revision readable") {
                val db = SqliteTestFixture(tempdir()).apply { initialize() }
                val revision = db.eventStore().append(trip, null, opening).committedRevision()
                val readOnly = SqliteGroupEventStore(db.dataSource { setReadOnly(true) }, fixedClock)
                val before = readOnly.load(trip)
                before?.revision shouldBe revision
                shouldThrow<GroupStoreUnavailable> { readOnly.append(trip, revision, changes("EUR")) }
                db.eventStore().load(trip) shouldBe before
            }
        }
    })

private fun SqliteTestFixture.refuseUsd() = execute(
    """
    CREATE TRIGGER refuse_usd BEFORE INSERT ON group_events
    WHEN NEW.currency = 'USD'
    BEGIN SELECT RAISE(IGNORE); END
    """.trimIndent(),
)

private fun DataSource.observeConnection(after: (String) -> Unit): DataSource = object : DataSource by this {
    override fun getConnection(): Connection {
        val actual = this@observeConnection.connection
        val connectionClass = Connection::class.java
        return Proxy.newProxyInstance(connectionClass.classLoader, arrayOf(connectionClass)) { _, method, args ->
            val result = try {
                method.invoke(actual, *(args ?: emptyArray()))
            } catch (failure: InvocationTargetException) {
                throw failure.targetException
            }
            after(method.name)
            result
        } as Connection
    }
}

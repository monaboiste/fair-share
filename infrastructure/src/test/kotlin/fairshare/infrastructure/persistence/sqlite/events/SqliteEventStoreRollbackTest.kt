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
import org.springframework.dao.DataAccessException
import org.springframework.transaction.TransactionSystemException
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

class SqliteEventStoreRollbackTest :
    ExpectSpec({
        val streamId = StreamId("original-stream")

        expect("a globally duplicate event ID rolls back an earlier insert and the new stream header") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val otherId = StreamId("other-stream")
            val other = pending(TestCreated("other"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))

            val failure = shouldThrow<DataAccessException> {
                database.store.append(otherId, StreamRevision.INITIAL, listOf(other, created))
            }
            failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE

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

                val probe = AppendConnectionProbe(database.dataSource)
                val store = database.store(probe)
                if (action == "aborts") {
                    val failure = shouldThrow<DataAccessException> {
                        store.append(streamId, StreamRevision(1), listOf(renamed, rejected))
                    }
                    failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                        SQLiteErrorCode.SQLITE_CONSTRAINT_TRIGGER
                } else {
                    shouldThrow<IllegalStateException> {
                        store.append(streamId, StreamRevision(1), listOf(renamed, rejected))
                    }
                }

                probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
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

        expect("a revision mismatch explicitly rolls back before closing its only connection") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val probe = AppendConnectionProbe(database.dataSource)
            val store = database.store(probe)
            val batch = listOf(pending(TestRenamed("conflict")))

            store.append(streamId, StreamRevision(1), batch) shouldBe AppendResult.RevisionMismatch

            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            database.reopen().load(streamId) shouldBe null
        }

        expect("rollback failure on revision mismatch escapes without retrying or committing") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val rollbackFailure = SQLException("Rollback failed")
            val probe = AppendConnectionProbe(database.dataSource, rollbackFailure = rollbackFailure)
            val store = database.store(probe)
            val batch = listOf(pending(TestRenamed("conflict")))

            val failure = shouldThrow<TransactionSystemException> {
                store.append(streamId, StreamRevision(1), batch)
            }
            failure.cause shouldBe rollbackFailure

            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            database.reopen().load(streamId) shouldBe null
        }

        expect("a rollback failure before JDBC rollback preserves the insert error without committing partial writes") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("conflict"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))
            val rollbackFailure = SQLException("Rollback failed")
            val probe = AppendConnectionProbe(database.dataSource, rollbackFailure = rollbackFailure)
            val store = database.store(probe)

            val failure = shouldThrow<DataAccessException> {
                store.append(streamId, StreamRevision(1), listOf(renamed, created))
            }

            failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
            failure.suppressed.toList() shouldBe listOf(rollbackFailure)
            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            database.reopen().load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }

        expect("an exception after a real commit leaves the outcome unknown even when the full batch is persisted") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("first of batch"))
            val renamedAgain = pending(TestRenamed("second of batch"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))
            val commitFailure = SQLException("Commit acknowledgement failed")
            val rollbackFailure = SQLException("Rollback failed")
            val probe = AppendConnectionProbe(database.dataSource, commitFailure, rollbackFailure)
            val store = database.store(probe)

            val failure = shouldThrow<UnknownEventCommitException> {
                store.append(streamId, StreamRevision(1), listOf(renamed, renamedAgain))
            }

            failure.cause shouldBe commitFailure
            failure.suppressed.toList() shouldBe listOf(rollbackFailure)
            probe.calls shouldBe listOf("open", "autoCommit=false", "commit", "rollback", "close")
            database.reopen().load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(2)
                events shouldBe listOf(
                    created.envelope(streamId, 1, 1),
                    renamed.envelope(streamId, 2, 2),
                    renamedAgain.envelope(streamId, 3, 2),
                )
            }
        }

        listOf(SQLException("Rollback did not execute"), IllegalStateException("Rollback did not execute"))
            .forEach { rollbackFailure ->
                expect(
                    "cleanup cannot commit a batch after commit and rollback fail with ${rollbackFailure.javaClass.simpleName}",
                ) {
                    val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
                    val created = pending(TestCreated("original"))
                    val commitFailure = SQLException("Commit did not execute")
                    val probe = AppendConnectionProbe(
                        database.dataSource,
                        beforeCommitFailure = commitFailure,
                        rollbackFailure = rollbackFailure,
                    )
                    val store = database.store(probe)

                    val failure = shouldThrow<UnknownEventCommitException> {
                        store.append(streamId, StreamRevision.INITIAL, listOf(created))
                    }

                    failure.cause shouldBe commitFailure
                    failure.suppressed.toList() shouldBe listOf(rollbackFailure)
                    probe.calls shouldBe listOf("open", "autoCommit=false", "commit", "rollback", "close")
                    database.reopen().load(streamId) shouldBe null
                }
            }

        expect("a rollback-journal reader preventing commit produces an unknown commit outcome") {
            val database = SqliteEventStoreFixture(
                tempdir().resolve("events.db"),
                busyTimeoutMillis = 0,
                journalMode = SQLiteConfig.JournalMode.DELETE,
            )
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

private class AppendConnectionProbe(
    private val delegate: DataSource,
    private val afterCommitFailure: SQLException? = null,
    private val rollbackFailure: Exception? = null,
    private val beforeCommitFailure: SQLException? = null,
) : DataSource by delegate {
    private val operations = mutableListOf<String>()
    val calls: List<String> get() = operations.toList()

    override fun getConnection(): Connection {
        val connection = delegate.connection
        operations += "open"
        return object : Connection by connection {
            override fun setAutoCommit(autoCommit: Boolean) {
                operations += "autoCommit=$autoCommit"
                connection.autoCommit = autoCommit
            }

            override fun commit() {
                operations += "commit"
                beforeCommitFailure?.let { throw it }
                connection.commit()
                afterCommitFailure?.let { throw it }
            }

            override fun rollback() {
                operations += "rollback"
                rollbackFailure?.let { throw it }
                connection.rollback()
            }

            override fun close() {
                operations += "close"
                connection.close()
            }
        }
    }
}

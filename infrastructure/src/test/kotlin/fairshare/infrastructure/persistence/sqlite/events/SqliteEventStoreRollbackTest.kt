package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import fairshare.infrastructure.persistence.sqlite.UnknownCommitOutcomeException
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
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val otherId = StreamId("other-stream")
            val other = pending(TestCreated("other"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))

            val failure = shouldThrow<DataAccessException> {
                store.append(otherId, StreamRevision.INITIAL, listOf(other, created))
            }
            failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE

            store.load(otherId) shouldBe null
            store.load(streamId).shouldNotBeNull().events shouldBe listOf(created.envelope(streamId, 1, 1))
            store.append(otherId, StreamRevision.INITIAL, listOf(other)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            store.load(otherId).shouldNotBeNull().events shouldBe listOf(other.envelope(otherId, 1, 1))
        }

        listOf("aborts" to "RAISE(ABORT, 'rejected event')", "ignores" to "RAISE(IGNORE)").forEach { (action, raise) ->
            expect("the whole append rolls back when a trigger $action its second insert") {
                val database = TestSqliteDatabase(tempdir().resolve("events.db"))
                val store = testEventStore(database.dataSource)
                val created = pending(TestCreated("original"))
                val renamed = pending(TestRenamed("first of batch"))
                val rejected = pending(TestRenamed("second of batch"))
                store.append(streamId, StreamRevision.INITIAL, listOf(created))
                database.executeSql(
                    """
                    CREATE TRIGGER reject_event BEFORE INSERT ON event
                    WHEN NEW.event_id = '${rejected.eventId.value}'
                    BEGIN SELECT $raise; END
                    """.trimIndent(),
                )

                val probe = AppendConnectionProbe(database.dataSource)
                val trackedStore = testEventStore(probe)
                if (action == "aborts") {
                    val failure = shouldThrow<DataAccessException> {
                        trackedStore.append(streamId, StreamRevision(1), listOf(renamed, rejected))
                    }
                    failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                        SQLiteErrorCode.SQLITE_CONSTRAINT_TRIGGER
                } else {
                    shouldThrow<IllegalStateException> {
                        trackedStore.append(streamId, StreamRevision(1), listOf(renamed, rejected))
                    }
                }

                probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
                store.load(streamId).shouldNotBeNull().apply {
                    revision shouldBe StreamRevision(1)
                    events shouldBe listOf(created.envelope(streamId, 1, 1))
                }
                database.executeSql("DROP TRIGGER reject_event")
                store.append(streamId, StreamRevision(1), listOf(renamed, rejected)) shouldBe
                    AppendResult.Committed(StreamRevision(2))
                store.load(streamId).shouldNotBeNull().events shouldBe listOf(
                    created.envelope(streamId, 1, 1),
                    renamed.envelope(streamId, 2, 2),
                    rejected.envelope(streamId, 3, 2),
                )
            }
        }

        expect("a revision mismatch explicitly rolls back before closing its only connection") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val probe = AppendConnectionProbe(database.dataSource)
            val store = testEventStore(probe)
            val batch = listOf(pending(TestRenamed("conflict")))

            store.append(streamId, StreamRevision(1), batch) shouldBe AppendResult.RevisionMismatch

            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            testEventStore(database.newDataSource()).load(streamId) shouldBe null
        }

        expect("rollback failure on revision mismatch escapes without retrying or committing") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val rollbackFailure = SQLException("Rollback failed")
            val probe = AppendConnectionProbe(database.dataSource, rollbackFailure = rollbackFailure)
            val store = testEventStore(probe)
            val batch = listOf(pending(TestRenamed("conflict")))

            val failure = shouldThrow<TransactionSystemException> {
                store.append(streamId, StreamRevision(1), batch)
            }
            failure.cause shouldBe rollbackFailure

            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            testEventStore(database.newDataSource()).load(streamId) shouldBe null
        }

        expect("a rollback failure before JDBC rollback preserves the insert error without committing partial writes") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("conflict"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))
            val rollbackFailure = SQLException("Rollback failed")
            val probe = AppendConnectionProbe(database.dataSource, rollbackFailure = rollbackFailure)
            val faultingStore = testEventStore(probe)

            val failure = shouldThrow<DataAccessException> {
                faultingStore.append(streamId, StreamRevision(1), listOf(renamed, created))
            }

            failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
            failure.suppressed.toList() shouldBe listOf(rollbackFailure)
            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            testEventStore(database.newDataSource()).load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }

        expect("an exception after a real commit leaves the outcome unknown even when the full batch is persisted") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("first of batch"))
            val renamedAgain = pending(TestRenamed("second of batch"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))
            val commitFailure = SQLException("Commit acknowledgement failed")
            val rollbackFailure = SQLException("Rollback failed")
            val probe = AppendConnectionProbe(
                database.dataSource,
                afterCommitFailure = commitFailure,
                rollbackFailure = rollbackFailure,
            )
            val faultingStore = testEventStore(probe)

            val failure = shouldThrow<UnknownCommitOutcomeException> {
                faultingStore.append(streamId, StreamRevision(1), listOf(renamed, renamedAgain))
            }

            failure.cause shouldBe commitFailure
            failure.suppressed.toList() shouldBe listOf(rollbackFailure)
            probe.calls shouldBe listOf("open", "autoCommit=false", "commit", "rollback", "close")
            testEventStore(database.newDataSource()).load(streamId).shouldNotBeNull().apply {
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
                    val database = TestSqliteDatabase(tempdir().resolve("events.db"))
                    val created = pending(TestCreated("original"))
                    val commitFailure = SQLException("Commit did not execute")
                    val probe = AppendConnectionProbe(
                        database.dataSource,
                        beforeCommitFailure = commitFailure,
                        rollbackFailure = rollbackFailure,
                    )
                    val store = testEventStore(probe)

                    val failure = shouldThrow<UnknownCommitOutcomeException> {
                        store.append(streamId, StreamRevision.INITIAL, listOf(created))
                    }

                    failure.cause shouldBe commitFailure
                    failure.suppressed.toList() shouldBe listOf(rollbackFailure)
                    probe.calls shouldBe listOf("open", "autoCommit=false", "commit", "rollback", "close")
                    testEventStore(database.newDataSource()).load(streamId) shouldBe null
                }
            }

        listOf(IllegalStateException("Commit did not execute"), TransactionSystemException("Commit did not execute"))
            .forEach { commitFailure ->
                expect("a commit ${commitFailure.javaClass.simpleName} leaves the outcome unknown after rollback") {
                    val database = TestSqliteDatabase(tempdir().resolve("events.db"))
                    val probe = AppendConnectionProbe(database.dataSource, beforeCommitFailure = commitFailure)
                    val store = testEventStore(probe)

                    val failure = shouldThrow<UnknownCommitOutcomeException> {
                        store.append(streamId, StreamRevision.INITIAL, listOf(pending(TestCreated("original"))))
                    }

                    (failure.cause === commitFailure) shouldBe true
                    failure.suppressed.toList() shouldBe emptyList()
                    probe.calls shouldBe listOf("open", "autoCommit=false", "commit", "rollback", "close")
                    testEventStore(database.newDataSource()).load(streamId) shouldBe null
                }
            }

        expect("a JVM error from commit escapes unchanged and cleanup does not restore auto-commit") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val commitError = LinkageError("Commit did not execute")
            val probe = AppendConnectionProbe(database.dataSource, beforeCommitFailure = commitError)
            val store = testEventStore(probe)

            val failure = shouldThrow<LinkageError> {
                store.append(streamId, StreamRevision.INITIAL, listOf(pending(TestCreated("original"))))
            }

            (failure === commitError) shouldBe true
            probe.calls shouldBe listOf("open", "autoCommit=false", "commit", "rollback", "close")
            testEventStore(database.newDataSource()).load(streamId) shouldBe null
        }

        expect("a JVM error from mismatch rollback escapes unchanged and cleanup does not restore auto-commit") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val rollbackError = LinkageError("Rollback did not execute")
            val probe = AppendConnectionProbe(database.dataSource, rollbackFailure = rollbackError)
            val store = testEventStore(probe)

            val failure = shouldThrow<LinkageError> {
                store.append(streamId, StreamRevision(1), listOf(pending(TestRenamed("conflict"))))
            }

            (failure === rollbackError) shouldBe true
            probe.calls shouldBe listOf("open", "autoCommit=false", "rollback", "close")
            testEventStore(database.newDataSource()).load(streamId) shouldBe null
        }

        expect("a rollback-journal reader preventing commit produces an unknown commit outcome") {
            val database = TestSqliteDatabase(
                tempdir().resolve("events.db"),
                busyTimeoutMillis = 0,
                journalMode = SQLiteConfig.JournalMode.DELETE,
            )
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("attempted under a read lock"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))

            database.dataSource.connection.use { reader ->
                reader.autoCommit = false
                reader.createStatement().use { statement ->
                    statement.executeQuery("SELECT revision FROM event_stream").use { rows ->
                        rows.next() shouldBe true
                        rows.getLong(1) shouldBe 1L
                    }
                }

                val failure = shouldThrow<UnknownCommitOutcomeException> {
                    store.append(streamId, StreamRevision(1), listOf(renamed))
                }
                failure.cause.shouldBeInstanceOf<SQLiteException>().resultCode shouldBe SQLiteErrorCode.SQLITE_BUSY
            }

            store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
            store.append(streamId, StreamRevision(1), listOf(renamed)) shouldBe
                AppendResult.Committed(StreamRevision(2))
        }
    })

private class AppendConnectionProbe(
    private val delegate: DataSource,
    private val afterCommitFailure: SQLException? = null,
    private val rollbackFailure: Throwable? = null,
    private val beforeCommitFailure: Throwable? = null,
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

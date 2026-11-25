package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import fairshare.infrastructure.persistence.sqlite.config.SqliteConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.transaction.TransactionDefinition
import java.time.Instant

class SqliteEventStoreTest :
    ExpectSpec({
        val streamId = StreamId("first-stream")

        expect("a nonexistent stream has no history") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)

            store.load(streamId) shouldBe null
        }

        expect("each batch advances the revision once and reloads exact envelopes in sequence order") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"), Instant.parse("2026-03-05T10:15:30.876543219Z"))
            val renamed = pending(TestRenamed("second"), Instant.parse("2026-03-05T10:15:30.123456781Z"))
            val renamedAgain = pending(TestRenamed("third"), Instant.parse("2026-03-05T10:15:29.111222333Z"))
            val initialEvents = listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))

            store.append(streamId, StreamRevision.INITIAL, listOf(created, renamed)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            val initial = store.load(streamId).shouldNotBeNull()
            initial.streamId shouldBe streamId
            initial.revision shouldBe StreamRevision(1)
            initial.events shouldBe initialEvents

            store.append(streamId, StreamRevision(1), listOf(renamedAgain)) shouldBe
                AppendResult.Committed(StreamRevision(2))
            val updated = store.load(streamId).shouldNotBeNull()
            updated.streamId shouldBe streamId
            updated.revision shouldBe StreamRevision(2)
            updated.events shouldBe initialEvents + renamedAgain.envelope(streamId, 3, 2)
        }

        expect("stale, future, and create-only revisions cannot change an existing stream") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("second"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))
            store.append(streamId, StreamRevision(1), listOf(renamed))

            listOf(StreamRevision.INITIAL, StreamRevision(1), StreamRevision(3)).forEach { expected ->
                store.append(streamId, expected, listOf(pending(TestRenamed("conflict")))) shouldBe
                    AppendResult.RevisionMismatch
            }

            store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(2)
                events shouldBe listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 2))
            }
        }

        expect("a positive expected revision cannot create a missing stream") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)

            store.append(streamId, StreamRevision(1), listOf(pending(TestCreated("missing")))) shouldBe
                AppendResult.RevisionMismatch
            store.load(streamId) shouldBe null
        }

        expect("different streams advance their revisions and sequences independently") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val otherId = StreamId("other-stream")
            val first = pending(TestCreated("first"))
            val other = pending(TestCreated("other"))
            val renamed = pending(TestRenamed("first renamed"))

            store.append(streamId, StreamRevision.INITIAL, listOf(first))
            store.append(otherId, StreamRevision.INITIAL, listOf(other)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            store.append(streamId, StreamRevision(1), listOf(renamed)) shouldBe
                AppendResult.Committed(StreamRevision(2))

            store.load(streamId).shouldNotBeNull().events shouldBe
                listOf(first.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 2))
            store.load(otherId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(other.envelope(otherId, 1, 1))
            }
        }

        expect("append rejects an ambient transaction without joining it or committing independently") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val transactions = SqliteConfiguration().run {
                transactionTemplate(transactionManager(database.dataSource))
            }

            transactions.executeWithoutResult { status ->
                val failure = shouldThrow<IllegalStateException> {
                    store.append(streamId, StreamRevision.INITIAL, listOf(created))
                }
                failure.message shouldBe "SQLite event append does not support an ambient transaction"
                store.load(streamId) shouldBe null
                testEventStore(database.newDataSource()).load(streamId) shouldBe null
                status.setRollbackOnly()
            }

            store.load(streamId) shouldBe null
            store.append(streamId, StreamRevision.INITIAL, listOf(created)) shouldBe
                AppendResult.Committed(StreamRevision(1))
        }

        expect("an append cannot write when the template does not start a physical transaction") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource) {
                propagationBehavior = TransactionDefinition.PROPAGATION_SUPPORTS
            }
            val created = pending(TestCreated("original"))

            val failure = shouldThrow<IllegalStateException> {
                store.append(streamId, StreamRevision.INITIAL, listOf(created))
            }

            failure.message shouldBe "SQLite event append requires a new physical transaction"
            testEventStore(database.newDataSource()).load(streamId) shouldBe null
        }

        expect("a new store instance can read and append to persisted history") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("after reopening"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))

            val reopened = testEventStore(database.newDataSource())
            reopened.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
            reopened.append(streamId, StreamRevision(1), listOf(renamed)) shouldBe
                AppendResult.Committed(StreamRevision(2))
            testEventStore(database.newDataSource()).load(streamId).shouldNotBeNull().events shouldBe
                listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 2))
        }

        expect("initializing the schema again preserves events and the stream revision") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created))

            database.initializeSchema()
            database.initializeSchema()

            store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }
    })

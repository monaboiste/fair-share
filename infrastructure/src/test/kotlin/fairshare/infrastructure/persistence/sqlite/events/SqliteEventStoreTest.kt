package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

class SqliteEventStoreTest :
    ExpectSpec({
        val streamId = StreamId("first-stream")

        expect("a nonexistent stream has no history") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))

            database.store.load(streamId) shouldBe null
        }

        expect("each batch advances the revision once and reloads exact envelopes in sequence order") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"), Instant.parse("2026-03-05T10:15:30.876543219Z"))
            val renamed = pending(TestRenamed("second"), Instant.parse("2026-03-05T10:15:30.123456781Z"))
            val renamedAgain = pending(TestRenamed("third"), Instant.parse("2026-03-05T10:15:29.111222333Z"))
            val initialEvents = listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))

            database.store.append(streamId, StreamRevision.INITIAL, listOf(created, renamed)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            val initial = database.store.load(streamId).shouldNotBeNull()
            initial.streamId shouldBe streamId
            initial.revision shouldBe StreamRevision(1)
            initial.events shouldBe initialEvents

            database.store.append(streamId, StreamRevision(1), listOf(renamedAgain)) shouldBe
                AppendResult.Committed(StreamRevision(2))
            val updated = database.store.load(streamId).shouldNotBeNull()
            updated.streamId shouldBe streamId
            updated.revision shouldBe StreamRevision(2)
            updated.events shouldBe initialEvents + renamedAgain.envelope(streamId, 3, 2)
        }

        expect("stale, future, and create-only revisions cannot change an existing stream") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("second"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))
            database.store.append(streamId, StreamRevision(1), listOf(renamed))

            listOf(StreamRevision.INITIAL, StreamRevision(1), StreamRevision(3)).forEach { expected ->
                database.store.append(streamId, expected, listOf(pending(TestRenamed("conflict")))) shouldBe
                    AppendResult.RevisionMismatch
            }

            database.store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(2)
                events shouldBe listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 2))
            }
        }

        expect("a positive expected revision cannot create a missing stream") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))

            database.store.append(streamId, StreamRevision(1), listOf(pending(TestCreated("missing")))) shouldBe
                AppendResult.RevisionMismatch
            database.store.load(streamId) shouldBe null
        }

        expect("different streams advance their revisions and sequences independently") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val otherId = StreamId("other-stream")
            val first = pending(TestCreated("first"))
            val other = pending(TestCreated("other"))
            val renamed = pending(TestRenamed("first renamed"))

            database.store.append(streamId, StreamRevision.INITIAL, listOf(first))
            database.store.append(otherId, StreamRevision.INITIAL, listOf(other)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            database.store.append(streamId, StreamRevision(1), listOf(renamed)) shouldBe
                AppendResult.Committed(StreamRevision(2))

            database.store.load(streamId).shouldNotBeNull().events shouldBe
                listOf(first.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 2))
            database.store.load(otherId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(other.envelope(otherId, 1, 1))
            }
        }

        expect("an empty batch cannot create a stream or advance an existing one") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            shouldThrow<IllegalArgumentException> {
                database.store.append(streamId, StreamRevision.INITIAL, emptyList())
            }
            database.store.load(streamId) shouldBe null

            val created = pending(TestCreated("original"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))
            shouldThrow<IllegalArgumentException> {
                database.store.append(streamId, StreamRevision(1), emptyList())
            }
            database.store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }

        expect("a new store instance can read and append to persisted history") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("after reopening"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))

            val reopened = database.reopen()
            reopened.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
            reopened.append(streamId, StreamRevision(1), listOf(renamed)) shouldBe
                AppendResult.Committed(StreamRevision(2))
            database.reopen().load(streamId).shouldNotBeNull().events shouldBe
                listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 2))
        }

        expect("initializing the schema again preserves events and the stream revision") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))

            SqliteEventSchema.initialize(database.dataSource)
            SqliteEventSchema.initialize(database.dataSource)

            database.store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }
    })

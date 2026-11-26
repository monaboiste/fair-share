package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.DomainEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SqliteEventStoreEncodingTest :
    ExpectSpec({
        val streamId = StreamId("encoded-stream")
        val missingStreamId = StreamId("missing-stream")

        expect("empty batches never open a connection or create or advance a stream") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            testEventStore(database.dataSource).append(streamId, StreamRevision.INITIAL, listOf(created))
            val resources = TestJdbcResources(database.dataSource)
            val store = testEventStore(resources)

            shouldThrow<IllegalArgumentException> {
                store.append(missingStreamId, StreamRevision.INITIAL, emptyList())
            }
            shouldThrow<IllegalArgumentException> {
                store.append(streamId, StreamRevision(1), emptyList())
            }

            resources.openedCount shouldBe 0
            val reopened = testEventStore(database.newDataSource())
            reopened.load(missingStreamId) shouldBe null
            reopened.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }

        expect("unencodable batches never open a connection or change history, even at a mismatched revision") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            testEventStore(database.dataSource).append(streamId, StreamRevision.INITIAL, listOf(created))
            val unsupported = pending(object : DomainEvent {})
            val batch = listOf(pending(TestRenamed("valid first event")), unsupported)
            val resources = TestJdbcResources(database.dataSource)
            val store = testEventStore(resources)

            shouldThrow<IllegalArgumentException> {
                store.append(missingStreamId, StreamRevision.INITIAL, batch)
            }
            shouldThrow<IllegalArgumentException> {
                store.append(streamId, StreamRevision(1), batch)
            }
            shouldThrow<IllegalArgumentException> {
                store.append(streamId, StreamRevision.INITIAL, batch)
            }

            resources.openedCount shouldBe 0
            val reopened = testEventStore(database.newDataSource())
            reopened.load(missingStreamId) shouldBe null
            reopened.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }

        expect("the largest supported format version round-trips and the next version is rejected before decoding") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val created = pending(TestCreated("original"))
            val delegate = TestEventCodec()
            val codec = object : EventCodec {
                override fun encode(event: DomainEvent): EncodedEvent =
                    delegate.encode(event).copy(version = Int.MAX_VALUE)

                override fun decode(event: EncodedEvent): DomainEvent {
                    event.version shouldBe Int.MAX_VALUE
                    return delegate.decode(event.copy(version = 1))
                }
            }
            val store = testEventStore(database.dataSource, codec)

            store.append(streamId, StreamRevision.INITIAL, listOf(created)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            store.load(streamId).shouldNotBeNull().events shouldBe listOf(created.envelope(streamId, 1, 1))

            database.executeSql("PRAGMA ignore_check_constraints = ON", "UPDATE event SET event_version = 2147483648")
            shouldThrow<InvalidEventHistoryException> { store.load(streamId) }
        }

        expect("append encodes its batch snapshot in order exactly once before opening a connection") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val resources = TestJdbcResources(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("next"))
            val batch = mutableListOf(created, renamed)
            val encoded = mutableListOf<DomainEvent>()
            val delegate = TestEventCodec()
            val codec = object : EventCodec by delegate {
                override fun encode(event: DomainEvent): EncodedEvent {
                    resources.openedCount shouldBe 0
                    batch.clear()
                    encoded += event
                    return delegate.encode(event)
                }
            }
            val store = testEventStore(resources, codec)

            store.append(streamId, StreamRevision.INITIAL, batch) shouldBe AppendResult.Committed(StreamRevision(1))
            encoded shouldBe listOf(created.payload, renamed.payload)
            resources.openedCount shouldBe 1
            testEventStore(database.newDataSource()).load(streamId)?.events shouldBe
                listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))
        }
    })

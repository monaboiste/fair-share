package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.DomainEvent
import fairshare.domain.events.EventStream
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit.SECONDS

class SqliteEventStoreConcurrencyTest :
    ExpectSpec({
        val streamId = StreamId("shared-stream")

        listOf(StreamRevision.INITIAL, StreamRevision(1)).forEach { expected ->
            expect("competing appends at revision ${expected.value} commit just one batch") {
                val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
                val original = if (expected == StreamRevision.INITIAL) {
                    emptyList()
                } else {
                    listOf(pending(TestCreated("original"))).also {
                        database.store.append(streamId, StreamRevision.INITIAL, it)
                    }
                }
                val batches = listOf("left", "right").map { name ->
                    listOf(pending(TestRenamed(name)), pending(TestRenamed("$name again")))
                }
                val nextRevision = expected.value + 1
                val committed = AppendResult.Committed(StreamRevision(nextRevision))
                val results = compete(database, streamId, expected, batches)

                results.count { it == committed } shouldBe 1
                results.count { it == AppendResult.RevisionMismatch } shouldBe 1
                val winner = batches[results.indexOf(committed)]
                database.store.load(streamId).shouldNotBeNull().apply {
                    revision shouldBe StreamRevision(nextRevision)
                    events shouldBe original.map { it.envelope(streamId, 1, 1) } +
                        winner.mapIndexed { index, event ->
                            event.envelope(streamId, original.size + index + 1L, nextRevision)
                        }
                }
            }
        }

        expect("an in-progress load sees one snapshot while a WAL writer commits a new batch") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            database.executeSql("PRAGMA journal_mode = WAL")
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("first rename"))
            val appendedDuringRead = pending(TestRenamed("concurrent rename"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created, renamed))
            val codec = PausingReadCodec()
            val reader = SqliteEventStore(database.dataSource, codec)

            val snapshot = Executors.newSingleThreadExecutor().use { executor ->
                val future = executor.submit<EventStream?> { reader.load(streamId) }
                try {
                    codec.firstRow.awaitTestSignal()
                    database.reopen().append(streamId, StreamRevision(1), listOf(appendedDuringRead)) shouldBe
                        AppendResult.Committed(StreamRevision(2))
                } finally {
                    codec.resumeReading.countDown()
                }
                future.awaitResult().shouldNotBeNull()
            }

            val initialEvents = listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))
            snapshot.revision shouldBe StreamRevision(1)
            snapshot.events shouldBe initialEvents
            database.store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(2)
                events shouldBe initialEvents + appendedDuringRead.envelope(streamId, 3, 2)
            }
        }
    })

private fun compete(
    database: SqliteEventStoreFixture,
    streamId: StreamId,
    expected: StreamRevision,
    batches: List<List<PendingEvent>>,
): List<AppendResult> = Executors.newFixedThreadPool(batches.size).use { executor ->
    val barrier = CyclicBarrier(batches.size)
    val appends = batches.map { batch ->
        val store = database.reopen()
        executor.submit<AppendResult> {
            barrier.await(10, SECONDS)
            store.append(streamId, expected, batch)
        }
    }
    appends.map { it.awaitResult() }
}

private class PausingReadCodec(private val codec: EventCodec = TestEventCodec()) : EventCodec by codec {
    val firstRow = CountDownLatch(1)
    val resumeReading = CountDownLatch(1)

    override fun decode(event: EncodedEvent): DomainEvent {
        val decoded = codec.decode(event)
        if (decoded is TestCreated) {
            firstRow.countDown()
            resumeReading.awaitTestSignal()
        }
        return decoded
    }
}

private fun CountDownLatch.awaitTestSignal() {
    check(await(10, SECONDS)) { "Timed out waiting for coordinated SQLite operation" }
}

private fun <T> Future<T>.awaitResult(): T = get(10, SECONDS)

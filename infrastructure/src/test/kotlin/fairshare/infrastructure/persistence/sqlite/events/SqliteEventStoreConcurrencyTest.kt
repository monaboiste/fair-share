package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.DomainEvent
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration.Companion.seconds

private val coordinationTimeout = 10.seconds

class SqliteEventStoreConcurrencyTest :
    ExpectSpec({
        val streamId = StreamId("shared-stream")

        listOf(StreamRevision.INITIAL, StreamRevision(1)).forEach { expected ->
            expect("competing appends at revision ${expected.value} commit just one batch") {
                val database = TestSqliteDatabase(tempdir().resolve("events.db"))
                val store = testEventStore(database.dataSource)
                val original = if (expected == StreamRevision.INITIAL) {
                    emptyList()
                } else {
                    listOf(pending(TestCreated("original"))).also {
                        store.append(streamId, StreamRevision.INITIAL, it)
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
                store.load(streamId).shouldNotBeNull().apply {
                    revision shouldBe StreamRevision(nextRevision)
                    events shouldBe original.map { it.envelope(streamId, 1, 1) } +
                        winner.mapIndexed { index, event ->
                            event.envelope(streamId, original.size + index + 1L, nextRevision)
                        }
                }
            }
        }

        expect("an in-progress load sees one snapshot while a WAL writer commits a new batch") {
            val database = TestSqliteDatabase(tempdir().resolve("events.db"))
            val store = testEventStore(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("first rename"))
            val appendedDuringRead = pending(TestRenamed("concurrent rename"))
            store.append(streamId, StreamRevision.INITIAL, listOf(created, renamed))
            val codec = PausingReadCodec()
            val reader = testEventStore(database.newDataSource(), codec)
            val writer = testEventStore(database.newDataSource())

            val snapshot = withTimeout(coordinationTimeout) {
                coroutineScope {
                    val reading = async(Dispatchers.IO) { reader.load(streamId) }
                    try {
                        codec.awaitFirstRow()
                        val writing = async(Dispatchers.IO) {
                            writer.append(streamId, StreamRevision(1), listOf(appendedDuringRead))
                        }
                        writing.await() shouldBe AppendResult.Committed(StreamRevision(2))
                    } finally {
                        codec.resumeReading()
                    }
                    reading.await().shouldNotBeNull()
                }
            }

            val initialEvents = listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))
            snapshot.revision shouldBe StreamRevision(1)
            snapshot.events shouldBe initialEvents
            store.load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(2)
                events shouldBe initialEvents + appendedDuringRead.envelope(streamId, 3, 2)
            }
        }
    })

private suspend fun compete(
    database: TestSqliteDatabase,
    streamId: StreamId,
    expected: StreamRevision,
    batches: List<List<PendingEvent>>,
): List<AppendResult> = withTimeout(coordinationTimeout) {
    coroutineScope {
        val ready = Channel<Unit>(Channel.RENDEZVOUS)
        val start = CompletableDeferred<Unit>()
        val appends = batches.map { batch ->
            val store = testEventStore(database.newDataSource())
            async(Dispatchers.IO) {
                ready.send(Unit)
                start.await()
                store.append(streamId, expected, batch)
            }
        }
        repeat(batches.size) { ready.receive() }
        start.complete(Unit)
        appends.awaitAll()
    }
}

private class PausingReadCodec(private val codec: EventCodec = TestEventCodec()) : EventCodec by codec {
    private val firstRow = CompletableDeferred<Unit>()
    private val resume = CountDownLatch(1)

    suspend fun awaitFirstRow() = firstRow.await()

    fun resumeReading() = resume.countDown()

    override fun decode(event: EncodedEvent): DomainEvent {
        val decoded = codec.decode(event)
        if (firstRow.complete(Unit)) {
            check(resume.await(coordinationTimeout.inWholeMilliseconds, MILLISECONDS)) {
                "Timed out waiting for the WAL writer before resuming the read"
            }
        }
        return decoded
    }
}

package fairshare.domain.events

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

class EventStreamTest : ExpectSpec() {
    init {
        expect("equal payloads recorded at the same time can receive distinct event identities") {
            val first = PendingEvent(EventId.random(), Instant.EPOCH, TestEvent)
            val second = first.copy(eventId = EventId.random())

            first.eventId shouldNotBe second.eventId
        }

        expect("stream identifiers cannot be blank") {
            listOf("", " \t\n").forEach { value ->
                shouldThrow<IllegalArgumentException> { StreamId(value) }
            }
        }

        expect("stream identifiers reject malformed Unicode rather than aliasing another stream") {
            shouldThrow<IllegalArgumentException> { StreamId("\uD800") }
            StreamId("stream-🧾").value shouldBe "stream-🧾"
        }

        expect("stream revisions cannot precede the initial revision") {
            shouldThrow<IllegalArgumentException> { StreamRevision(-1) }
        }

        expect("replay preserves supplied sequence order across append batches regardless of timestamp") {
            val history = listOf(
                envelope(1, 1, recordedAt = Instant.EPOCH.plusSeconds(1)),
                envelope(2, 1),
                envelope(3, 2),
            )

            EventStream(streamId, StreamRevision(2), history).events shouldBe history
        }

        expect("stream identities distinguish independent histories") {
            val otherId = StreamId("other")
            val event = envelope(1, 1)
            val streams = listOf(
                EventStream(streamId, StreamRevision(1), listOf(event)),
                EventStream(
                    otherId,
                    StreamRevision(1),
                    listOf(event.copy(eventId = EventId.random(), streamId = otherId)),
                ),
            )

            streams.associateBy { it.streamId }.keys.map { it.value }.toSet() shouldBe setOf("stream", "other")
        }

        expect("a replayed history can be extended at its next revision") {
            val first = envelope(1, 1)
            val stream = EventStream(streamId, StreamRevision(1), listOf(first))
            val nextRevision = StreamRevision(stream.revision.value + 1)
            val nextEvent = envelope(2, 2)

            val extended = EventStream(stream.streamId, nextRevision, stream.events + nextEvent)

            extended.events shouldBe listOf(first, nextEvent)
        }

        expect("changing the input list after construction cannot change recorded history") {
            val event = envelope(1, 1)
            val input = mutableListOf(event)
            val stream = EventStream(streamId, StreamRevision(1), input)

            input.clear()

            stream.events shouldBe listOf(event)
        }

        expect("casting the exposed history to a mutable list cannot modify it") {
            val history = listOf(envelope(1, 1), envelope(2, 1))
            val stream = EventStream(streamId, StreamRevision(1), history)

            shouldThrow<UnsupportedOperationException> {
                (stream.events as MutableList<EventEnvelope>)[0] = history.last()
            }

            stream.events shouldBe history
        }

        expect("a recorded stream rejects invalid history") {
            val first = envelope(1, 1)
            val second = envelope(2, 2)
            val invalidHistories = mapOf(
                "empty history" to invalidHistory(1),
                "initial stream revision" to InvalidHistory(StreamRevision.INITIAL, listOf(first)),
                "event from a different stream" to invalidHistory(2, first, second.copy(streamId = StreamId("other"))),
                "sequence starts at zero" to invalidHistory(1, first.copy(sequence = 0)),
                "sequence skips a position" to invalidHistory(2, first, second.copy(sequence = 3)),
                "sequence repeats a position" to invalidHistory(2, first, second.copy(sequence = 1)),
                "events arrive out of sequence" to invalidHistory(1, envelope(2, 1), first),
                "first event uses the initial revision" to invalidHistory(1, envelope(1, 0), envelope(2, 1)),
                "first event skips revision one" to invalidHistory(2, envelope(1, 2)),
                "an append batch skips a revision" to invalidHistory(3, first, envelope(2, 3)),
                "an append batch goes back to an earlier revision" to invalidHistory(1, first, second, envelope(3, 1)),
                "stream revision is ahead of its final event" to invalidHistory(2, first),
                "stream revision is behind its final event" to invalidHistory(1, first, second),
                "event identifier is reused in a later batch" to
                    invalidHistory(2, first, second.copy(eventId = first.eventId)),
            )
            invalidHistories.forEach { (description, history) ->
                withClue(description) {
                    shouldThrow<IllegalArgumentException> { EventStream(streamId, history.revision, history.events) }
                }
            }
        }
    }
}

private val streamId = StreamId("stream")

private fun envelope(sequence: Long, revision: Long, recordedAt: Instant = Instant.EPOCH): EventEnvelope =
    EventEnvelope(EventId.random(), streamId, sequence, StreamRevision(revision), recordedAt, TestEvent)

private fun invalidHistory(revision: Long, vararg events: EventEnvelope): InvalidHistory =
    InvalidHistory(StreamRevision(revision), events.toList())

private data object TestEvent : DomainEvent

private data class InvalidHistory(val revision: StreamRevision, val events: List<EventEnvelope>)

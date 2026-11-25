package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent
import fairshare.domain.events.EventEnvelope
import fairshare.domain.events.EventId
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import java.time.Instant

internal fun pending(
    payload: DomainEvent,
    recordedAt: Instant = Instant.parse("2026-03-05T10:15:30.123456789Z"),
): PendingEvent = PendingEvent(EventId.random(), recordedAt, payload)

internal fun PendingEvent.envelope(streamId: StreamId, sequence: Long, revision: Long): EventEnvelope = EventEnvelope(
    eventId = eventId,
    streamId = streamId,
    sequence = sequence,
    revision = StreamRevision(revision),
    recordedAt = recordedAt,
    payload = payload,
)

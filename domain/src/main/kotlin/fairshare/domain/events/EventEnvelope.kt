package fairshare.domain.events

import java.time.Instant

/** A recorded event with its one-based position ([sequence]) and append batch [revision] within the stream. */
public data class EventEnvelope(
    val eventId: EventId,
    val streamId: StreamId,
    val sequence: Long,
    val revision: StreamRevision,
    val recordedAt: Instant,
    val payload: DomainEvent,
)

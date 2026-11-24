package fairshare.domain.events

import java.time.Instant

public data class EventEnvelope(
    val eventId: EventId,
    val streamId: StreamId,
    val sequence: Long,
    val revision: StreamRevision,
    val recordedAt: Instant,
    val payload: DomainEvent,
)

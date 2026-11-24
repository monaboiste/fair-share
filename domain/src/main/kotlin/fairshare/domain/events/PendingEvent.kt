package fairshare.domain.events

import java.time.Instant

public data class PendingEvent(val eventId: EventId, val recordedAt: Instant, val payload: DomainEvent)

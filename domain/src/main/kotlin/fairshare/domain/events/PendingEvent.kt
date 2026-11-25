package fairshare.domain.events

import java.time.Instant

/** Event data supplied for append before its stream sequence and batch revision are assigned. */
public data class PendingEvent(val eventId: EventId, val recordedAt: Instant, val payload: DomainEvent)

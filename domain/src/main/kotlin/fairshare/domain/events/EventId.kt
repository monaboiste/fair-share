package fairshare.domain.events

import java.util.UUID

@JvmInline
public value class EventId(public val value: UUID) {
    public companion object {
        public fun random(): EventId = EventId(UUID.randomUUID())
    }
}

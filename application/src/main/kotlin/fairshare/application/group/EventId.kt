package fairshare.application.group

import java.util.UUID

public data class EventId(public val value: String) {
    public companion object {
        public fun random(): EventId = EventId("${UUID.randomUUID()}")
    }
}

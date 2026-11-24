package fairshare.application.commands.ports

import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision

public fun interface AppendEvents {
    public fun append(streamId: StreamId, expectedRevision: StreamRevision, events: List<PendingEvent>): AppendResult
}

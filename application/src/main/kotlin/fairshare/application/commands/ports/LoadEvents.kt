package fairshare.application.commands.ports

import fairshare.domain.events.EventStream
import fairshare.domain.events.StreamId

public fun interface LoadEvents {
    public fun load(streamId: StreamId): EventStream?
}

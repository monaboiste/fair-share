package fairshare.application.commands.ports

import fairshare.domain.events.EventStream
import fairshare.domain.events.StreamId

/** Reads recorded event history. */
public fun interface LoadEvents {
    /**
     * Returns one complete, sequence-ordered snapshot, or `null` if the stream does not exist.
     * Invalid recorded history fails the load rather than returning a partial snapshot.
     */
    public fun load(streamId: StreamId): EventStream?
}

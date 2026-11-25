package fairshare.application.commands.ports

import fairshare.domain.events.EventStream
import fairshare.domain.events.StreamId

/** Reads recorded event history. */
public fun interface LoadEvents {
    /**
     * Returns the complete event history, or `null` if the stream does not exist.
     *
     * **Implementation requirements:**
     *
     * - Read stream metadata and events from a consistent snapshot.
     * - Return events in sequence order.
     * - Reject invalid history instead of returning partial results.
     */
    public fun load(streamId: StreamId): EventStream?
}

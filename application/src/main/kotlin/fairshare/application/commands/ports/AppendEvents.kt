package fairshare.application.commands.ports

import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision

/** Writes event batches with optimistic concurrency control. */
public fun interface AppendEvents {
    /**
     * Atomically records a nonempty batch at the expected stream revision.
     *
     * **Implementation requirements:**
     *
     * - Persist all events or none.
     * - Preserve event order and metadata.
     * - Advance the stream revision exactly once per batch.
     * - Return [AppendResult.Committed] only after a durable commit.
     * - Leave persisted state unchanged on revision mismatch.
     * - Never automatically retry an append.
     *
     * @throws IllegalArgumentException if [events] is empty.
     */
    public fun append(streamId: StreamId, expectedRevision: StreamRevision, events: List<PendingEvent>): AppendResult
}

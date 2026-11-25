package fairshare.application.commands.ports

import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision

/** Writes event batches with optimistic concurrency control. */
public fun interface AppendEvents {
    /**
     * Atomically records a nonempty batch in input order and advances the stream revision once.
     * [StreamRevision.INITIAL] requires a missing stream; any other expected revision must match an existing stream.
     * A mismatch writes nothing. An unconfirmed commit is reported as a failure, but the batch may be durable.
     *
     * @throws IllegalArgumentException if [events] is empty.
     */
    public fun append(streamId: StreamId, expectedRevision: StreamRevision, events: List<PendingEvent>): AppendResult
}

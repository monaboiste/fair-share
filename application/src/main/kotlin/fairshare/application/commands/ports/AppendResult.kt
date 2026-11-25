package fairshare.application.commands.ports

import fairshare.domain.events.StreamRevision

/** Confirmed outcomes of an append attempt. Persistence failures are reported separately. */
public sealed interface AppendResult {
    /** The entire batch committed at [revision]. */
    public data class Committed(public val revision: StreamRevision) : AppendResult {
        init {
            require(revision != StreamRevision.INITIAL) { "Committed revision must be positive" }
        }
    }

    /** The expected revision did not match and no events were written by this attempt. */
    public data object RevisionMismatch : AppendResult
}

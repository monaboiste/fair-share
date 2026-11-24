package fairshare.application.commands.ports

import fairshare.domain.events.StreamRevision

public sealed interface AppendResult {
    public data class Committed(public val revision: StreamRevision) : AppendResult {
        init {
            require(revision != StreamRevision.INITIAL) { "Committed revision must be positive" }
        }
    }

    public data object RevisionMismatch : AppendResult
}

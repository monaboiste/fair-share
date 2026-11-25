package fairshare.infrastructure.persistence.sqlite.events

/** Persisted event payload with a stable nonblank type identifier and a positive format version. */
internal data class EncodedEvent(val type: String, val version: Int, val payload: String)

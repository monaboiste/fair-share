package fairshare.infrastructure.persistence.sqlite.events

internal data class EncodedEvent(val type: String, val version: Int, val payload: String)

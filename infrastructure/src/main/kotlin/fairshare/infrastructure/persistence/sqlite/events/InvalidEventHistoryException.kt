package fairshare.infrastructure.persistence.sqlite.events

/** Recorded history cannot be loaded as a valid event stream. */
internal class InvalidEventHistoryException(message: String, cause: RuntimeException) :
    RuntimeException(message, cause)

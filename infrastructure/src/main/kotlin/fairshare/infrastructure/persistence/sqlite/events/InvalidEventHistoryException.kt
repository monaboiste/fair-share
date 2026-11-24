package fairshare.infrastructure.persistence.sqlite.events

internal class InvalidEventHistoryException(message: String, cause: RuntimeException) :
    RuntimeException(message, cause)

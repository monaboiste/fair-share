package fairshare.infrastructure.persistence.sqlite.events

import org.springframework.transaction.TransactionSystemException

internal class UnknownEventCommitException(message: String, cause: Throwable) :
    TransactionSystemException(message, cause)

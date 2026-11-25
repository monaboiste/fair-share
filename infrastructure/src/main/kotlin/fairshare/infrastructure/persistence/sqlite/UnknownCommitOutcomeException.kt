package fairshare.infrastructure.persistence.sqlite

import org.springframework.transaction.TransactionSystemException

internal class UnknownCommitOutcomeException(message: String, cause: Throwable) :
    TransactionSystemException(message, cause)

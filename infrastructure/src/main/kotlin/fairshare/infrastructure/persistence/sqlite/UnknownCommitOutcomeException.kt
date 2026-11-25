package fairshare.infrastructure.persistence.sqlite

import org.springframework.transaction.TransactionSystemException

/** Commit could not be confirmed. Writes may be durable; reload and inspect state before resubmitting. */
internal class UnknownCommitOutcomeException(message: String, cause: Throwable) :
    TransactionSystemException(message, cause)

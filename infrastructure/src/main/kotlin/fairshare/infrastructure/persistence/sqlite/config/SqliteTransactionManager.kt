package fairshare.infrastructure.persistence.sqlite.config

import fairshare.infrastructure.persistence.sqlite.UnknownCommitOutcomeException
import org.springframework.jdbc.datasource.ConnectionHolder
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import javax.sql.DataSource

/**
 * For unpooled SQLite connections only. Close the physical connection without restoring auto-commit,
 * which could commit unfinished writes after a failed rollback or commit. Handle commit failures here
 * so Spring cannot replace an unknown commit outcome with a rollback failure. JDBC wrappers can
 * fail with unchecked exceptions too; handle those at the same boundaries as SQL failures.
 */
internal class SqliteTransactionManager(dataSource: DataSource) : DataSourceTransactionManager(dataSource) {
    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        check(!TransactionSynchronizationManager.hasResource(obtainDataSource())) {
            "A new SQLite transaction requires an unbound connection"
        }
        super.doBegin(transaction, definition)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun doCommit(status: DefaultTransactionStatus) {
        try {
            super.doCommit(status)
        } catch (failure: RuntimeException) {
            val unknown = UnknownCommitOutcomeException("SQLite commit outcome unknown", jdbcFailure(failure))
            try {
                super.doRollback(status)
            } catch (rollback: RuntimeException) {
                unknown.addSuppressed(jdbcFailure(rollback))
            }
            throw unknown
        } catch (failure: Error) {
            TransactionSynchronizationManager.bindResource(status.transaction, failure)
            throw failure
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun doRollback(status: DefaultTransactionStatus) {
        try {
            super.doRollback(status)
        } catch (failure: RuntimeException) {
            val cause = jdbcFailure(failure)
            rethrowCommitErrorIfPresent(status, cause)
            throw TransactionSystemException("SQLite rollback failed", cause)
        } catch (failure: Error) {
            rethrowCommitErrorIfPresent(status, failure)
            throw failure
        }
    }

    private fun rethrowCommitErrorIfPresent(status: DefaultTransactionStatus, rollbackFailure: Throwable) {
        val commitError = TransactionSynchronizationManager.getResource(status.transaction) as? Error ?: return
        commitError.addSuppressed(rollbackFailure)
        throw commitError
    }

    override fun doCleanupAfterCompletion(transaction: Any) {
        TransactionSynchronizationManager.unbindResourceIfPossible(transaction)
        val dataSource = obtainDataSource()
        val holder = TransactionSynchronizationManager.unbindResource(dataSource) as ConnectionHolder
        DataSourceUtils.releaseConnection(holder.connection, dataSource)
        holder.clear()
    }

    private fun jdbcFailure(failure: RuntimeException): Throwable =
        (failure as? TransactionSystemException)?.cause ?: failure
}

package fairshare.infrastructure.persistence.sqlite.config

import fairshare.infrastructure.persistence.sqlite.events.UnknownEventCommitException
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
 * so Spring cannot replace an unknown commit outcome with a rollback failure.
 */
internal class SqliteTransactionManager(dataSource: DataSource) : DataSourceTransactionManager(dataSource) {
    override fun doBegin(transaction: Any, definition: TransactionDefinition) {
        check(!TransactionSynchronizationManager.hasResource(obtainDataSource())) {
            "A new SQLite transaction requires an unbound connection"
        }
        super.doBegin(transaction, definition)
    }

    override fun doCommit(status: DefaultTransactionStatus) {
        runCatching { super.doCommit(status) }.getOrElse { failure ->
            val unknown = UnknownEventCommitException("SQLite commit outcome unknown", jdbcFailure(failure))
            runCatching { super.doRollback(status) }.exceptionOrNull()?.let { rollback ->
                unknown.addSuppressed(jdbcFailure(rollback))
            }
            throw unknown
        }
    }

    override fun doRollback(status: DefaultTransactionStatus) {
        runCatching { super.doRollback(status) }.getOrElse { failure ->
            throw TransactionSystemException("SQLite rollback failed", jdbcFailure(failure))
        }
    }

    override fun doCleanupAfterCompletion(transaction: Any) {
        val dataSource = obtainDataSource()
        val holder = TransactionSynchronizationManager.unbindResource(dataSource) as ConnectionHolder
        DataSourceUtils.releaseConnection(holder.connection, dataSource)
        holder.clear()
    }

    private fun jdbcFailure(failure: Throwable): Throwable = (failure as? TransactionSystemException)?.cause ?: failure
}

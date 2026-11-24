package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupStoreUnavailable
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

internal fun <T> readDatabase(dataSource: DataSource, operation: (Connection) -> T): T = try {
    useJdbc(dataSource.connection, operation)
} catch (failure: SQLException) {
    throw GroupStoreUnavailable(failure)
}

internal fun <R : AutoCloseable, T> useJdbc(resource: R?, operation: (R) -> T): T =
    (resource ?: throw SQLException("JDBC did not return a resource")).use(operation)

internal fun rollbackAfterFailure(connection: Connection, primary: SQLException) {
    try {
        connection.rollback()
    } catch (failure: SQLException) {
        primary.addSuppressed(failure)
    }
}

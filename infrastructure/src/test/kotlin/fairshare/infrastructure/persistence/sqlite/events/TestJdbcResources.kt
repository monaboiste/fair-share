package fairshare.infrastructure.persistence.sqlite.events

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

internal class TestJdbcResources(
    private val delegate: DataSource,
    private val afterCloseFailure: SQLException? = null,
) : DataSource by delegate {
    private val pendingClose = mutableSetOf<AutoCloseable>()
    val allClosed: Boolean get() = pendingClose.isEmpty()
    var openedCount = 0
        private set

    override fun getConnection(): Connection {
        val connection = record(delegate.connection)
        openedCount++
        return object : Connection by connection {
            override fun createStatement(): Statement = track(connection.createStatement())

            override fun prepareStatement(sql: String): PreparedStatement = track(connection.prepareStatement(sql))

            override fun close() {
                release(connection)
                afterCloseFailure?.let { throw it }
            }
        }
    }

    private fun track(statement: Statement): Statement {
        record(statement)
        return object : Statement by statement {
            override fun executeQuery(sql: String): ResultSet = track(statement.executeQuery(sql))

            override fun close() = release(statement)
        }
    }

    private fun track(statement: PreparedStatement): PreparedStatement {
        record(statement)
        return object : PreparedStatement by statement {
            override fun executeQuery(): ResultSet = track(statement.executeQuery())

            override fun close() = release(statement)
        }
    }

    private fun track(rows: ResultSet): ResultSet {
        record(rows)
        return object : ResultSet by rows {
            override fun close() = release(rows)
        }
    }

    private fun <T : AutoCloseable> record(resource: T): T = resource.also { pendingClose += it }

    private fun release(resource: AutoCloseable) {
        resource.close()
        pendingClose -= resource
    }
}

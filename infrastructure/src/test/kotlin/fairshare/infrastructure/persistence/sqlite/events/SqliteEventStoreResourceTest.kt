package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

class SqliteEventStoreResourceTest :
    ExpectSpec({
        val streamId = StreamId("tracked-stream")

        expect("schema initialization, successful appends, mismatches, and loads close their JDBC resources") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val tracked = TrackedDataSource(database.dataSource)
            val store = SqliteEventStore(tracked, TestEventCodec())
            val created = pending(TestCreated("original"))

            SqliteEventSchema.initialize(tracked)
            store.append(streamId, StreamRevision.INITIAL, listOf(created)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            store.append(streamId, StreamRevision.INITIAL, listOf(created)) shouldBe AppendResult.RevisionMismatch
            store.load(streamId)?.events shouldBe listOf(created.envelope(streamId, 1, 1))

            tracked.openCount shouldBe 0
            database.executeSql("UPDATE event SET payload = '{'")
            shouldThrow<InvalidEventHistoryException> { store.load(streamId) }
            tracked.openCount shouldBe 0
        }

        expect("a JDBC close exception after successful commit does not change the append result") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val tracked = TrackedDataSource(database.dataSource, failOnClose = true)
            val store = SqliteEventStore(tracked, TestEventCodec())
            val created = pending(TestCreated("original"))

            store.append(streamId, StreamRevision.INITIAL, listOf(created)) shouldBe
                AppendResult.Committed(StreamRevision(1))
            database.store.load(streamId)?.events shouldBe listOf(created.envelope(streamId, 1, 1))
            tracked.openCount shouldBe 0
        }
    })

private class TrackedDataSource(private val delegate: DataSource, private val failOnClose: Boolean = false) :
    DataSource by delegate {
    var openCount = 0
        private set

    override fun getConnection(): Connection {
        val connection = delegate.connection
        openCount++
        return object : Connection by connection {
            override fun createStatement(): Statement = track(connection.createStatement())

            override fun prepareStatement(sql: String): PreparedStatement = track(connection.prepareStatement(sql))

            override fun close() {
                connection.close()
                openCount--
                if (failOnClose) throw SQLException("Simulated cleanup failure after closing the connection")
            }
        }
    }

    private fun track(statement: Statement): Statement {
        openCount++
        return object : Statement by statement {
            override fun close() {
                statement.close()
                openCount--
            }
        }
    }

    private fun track(statement: PreparedStatement): PreparedStatement {
        openCount++
        return object : PreparedStatement by statement {
            override fun executeQuery(): ResultSet = track(statement.executeQuery())

            override fun close() {
                statement.close()
                openCount--
            }
        }
    }

    private fun track(rows: ResultSet): ResultSet {
        openCount++
        return object : ResultSet by rows {
            override fun close() {
                rows.close()
                openCount--
            }
        }
    }
}

package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendResult
import fairshare.domain.events.DomainEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.dao.DataAccessException
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

            tracked.openedConnections shouldBe 4
            tracked.preparedStatements shouldBe 4
            tracked.queryResults shouldBe 3
            tracked.openCount shouldBe 0
            database.executeSql("UPDATE event SET payload = '{'")
            shouldThrow<InvalidEventHistoryException> { store.load(streamId) }
            tracked.openCount shouldBe 0
        }

        expect("a failed event insert closes the Spring JDBC resources and its dedicated connection") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val tracked = TrackedDataSource(database.dataSource)
            val store = SqliteEventStore(tracked, TestEventCodec())
            val duplicate = pending(TestCreated("duplicate"))

            shouldThrow<DataAccessException> {
                store.append(streamId, StreamRevision.INITIAL, listOf(duplicate, duplicate))
            }

            tracked.openedConnections shouldBe 1
            tracked.preparedStatements shouldBe 3
            tracked.queryResults shouldBe 1
            tracked.openCount shouldBe 0
            database.reopen().load(streamId) shouldBe null
        }

        expect("empty and unencodable batches never open a connection or change history") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val missingStreamId = StreamId("missing-stream")
            val created = pending(TestCreated("original"))
            database.store.append(streamId, StreamRevision.INITIAL, listOf(created))
            val unsupported = pending(object : DomainEvent {})
            val batch = listOf(pending(TestRenamed("valid first event")), unsupported)
            val tracked = TrackedDataSource(database.dataSource)
            val store = SqliteEventStore(tracked, TestEventCodec())

            shouldThrow<IllegalArgumentException> {
                store.append(missingStreamId, StreamRevision.INITIAL, emptyList())
            }
            shouldThrow<IllegalArgumentException> {
                store.append(missingStreamId, StreamRevision.INITIAL, batch)
            }
            shouldThrow<IllegalArgumentException> {
                store.append(streamId, StreamRevision(1), batch)
            }
            shouldThrow<IllegalArgumentException> {
                store.append(streamId, StreamRevision.INITIAL, batch)
            }

            tracked.openedConnections shouldBe 0
            database.reopen().load(missingStreamId) shouldBe null
            database.reopen().load(streamId).shouldNotBeNull().apply {
                revision shouldBe StreamRevision(1)
                events shouldBe listOf(created.envelope(streamId, 1, 1))
            }
        }

        expect("append encodes its batch snapshot in order exactly once before opening a connection") {
            val database = SqliteEventStoreFixture(tempdir().resolve("events.db"))
            val tracked = TrackedDataSource(database.dataSource)
            val created = pending(TestCreated("original"))
            val renamed = pending(TestRenamed("next"))
            val batch = mutableListOf(created, renamed)
            val encoded = mutableListOf<DomainEvent>()
            val delegate = TestEventCodec()
            val codec = object : EventCodec by delegate {
                override fun encode(event: DomainEvent): EncodedEvent {
                    tracked.openedConnections shouldBe 0
                    batch.clear()
                    encoded += event
                    return delegate.encode(event)
                }
            }
            val store = SqliteEventStore(tracked, codec)

            store.append(streamId, StreamRevision.INITIAL, batch) shouldBe AppendResult.Committed(StreamRevision(1))
            encoded shouldBe listOf(created.payload, renamed.payload)
            tracked.openedConnections shouldBe 1
            database.reopen().load(streamId)?.events shouldBe
                listOf(created.envelope(streamId, 1, 1), renamed.envelope(streamId, 2, 1))
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
    var openedConnections = 0
        private set
    var preparedStatements = 0
        private set
    var queryResults = 0
        private set

    override fun getConnection(): Connection {
        val connection = delegate.connection
        openedConnections++
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
        preparedStatements++
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
        queryResults++
        openCount++
        return object : ResultSet by rows {
            override fun close() {
                rows.close()
                openCount--
            }
        }
    }
}

package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendEvents
import fairshare.application.commands.ports.AppendResult
import fairshare.application.commands.ports.LoadEvents
import fairshare.domain.events.EventStream
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

internal class SqliteEventStore(private val dataSource: DataSource, private val eventCodec: EventCodec) :
    LoadEvents,
    AppendEvents {

    override fun load(streamId: StreamId): EventStream? = dataSource.connection.use { connection ->
        connection.prepareStatement(SqliteEventQueries.LOAD).use { statement ->
            statement.setString(1, streamId.value)
            statement.executeQuery().use { rows -> readEventStream(streamId, rows, eventCodec) }
        }
    }

    override fun append(
        streamId: StreamId,
        expectedRevision: StreamRevision,
        events: List<PendingEvent>,
    ): AppendResult {
        val batch = events.toList()
        require(batch.isNotEmpty()) { "An event append must not be empty" }

        var committed: AppendResult.Committed? = null
        return try {
            dataSource.connection.use { connection ->
                connection.autoCommit = false
                val position = claim(connection, streamId, expectedRevision, batch.size)
                    ?: return@use AppendResult.RevisionMismatch
                insertEvents(connection, streamId, position, batch)
                val result = AppendResult.Committed(position.revision)
                commit(connection, streamId)
                committed = result
                result
            }
        } catch (failure: SQLException) {
            val result = committed ?: throw failure
            logger.warn("Could not close SQLite connection after committing stream {}", streamId.value, failure)
            result
        }
    }

    private fun claim(
        connection: Connection,
        streamId: StreamId,
        expectedRevision: StreamRevision,
        batchSize: Int,
    ): ClaimedPosition? {
        val creating = expectedRevision == StreamRevision.INITIAL
        val sql = if (creating) SqliteEventQueries.CREATE else SqliteEventQueries.ADVANCE
        return connection.prepareStatement(sql).use { statement ->
            if (creating) {
                statement.setString(1, streamId.value)
                statement.setInt(2, batchSize)
            } else {
                statement.setInt(1, batchSize)
                statement.setString(2, streamId.value)
                statement.setLong(EXPECTED_REVISION_PARAMETER, expectedRevision.value)
            }
            statement.executeQuery().use { rows ->
                if (rows.next()) {
                    ClaimedPosition(
                        StreamRevision(rows.getLong("revision")),
                        rows.getLong("last_sequence") - batchSize + 1,
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun insertEvents(
        connection: Connection,
        streamId: StreamId,
        position: ClaimedPosition,
        events: List<PendingEvent>,
    ) {
        connection.prepareStatement(SqliteEventQueries.INSERT).use { statement ->
            for (index in events.indices) {
                val event = events[index]
                val encoded = eventCodec.encode(event.payload)
                statement.setString(EVENT_ID_PARAMETER, event.eventId.value.toString())
                statement.setString(STREAM_ID_PARAMETER, streamId.value)
                statement.setLong(SEQUENCE_PARAMETER, position.firstSequence + index)
                statement.setLong(REVISION_PARAMETER, position.revision.value)
                statement.setString(RECORDED_AT_PARAMETER, event.recordedAt.toString())
                statement.setString(EVENT_TYPE_PARAMETER, encoded.type)
                statement.setInt(EVENT_VERSION_PARAMETER, encoded.version)
                statement.setString(PAYLOAD_PARAMETER, encoded.payload)
                check(statement.executeUpdate() == 1) { "An event insert must persist exactly one row" }
            }
        }
    }

    private fun commit(connection: Connection, streamId: StreamId) {
        try {
            connection.commit()
        } catch (failure: SQLException) {
            throw UnknownEventCommitException("Commit outcome unknown for stream ${streamId.value}", failure)
        }
    }

    private class ClaimedPosition(val revision: StreamRevision, val firstSequence: Long)

    private companion object {
        const val EXPECTED_REVISION_PARAMETER = 3

        const val EVENT_ID_PARAMETER = 1
        const val STREAM_ID_PARAMETER = 2
        const val SEQUENCE_PARAMETER = 3
        const val REVISION_PARAMETER = 4
        const val RECORDED_AT_PARAMETER = 5
        const val EVENT_TYPE_PARAMETER = 6
        const val EVENT_VERSION_PARAMETER = 7
        const val PAYLOAD_PARAMETER = 8

        val logger: Logger = LoggerFactory.getLogger(SqliteEventStore::class.java)
    }
}

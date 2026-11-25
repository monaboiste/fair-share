package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendEvents
import fairshare.application.commands.ports.AppendResult
import fairshare.application.commands.ports.LoadEvents
import fairshare.domain.events.EventId
import fairshare.domain.events.EventStream
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

internal class SqliteEventStore(private val dataSource: DataSource, private val eventCodec: EventCodec) :
    LoadEvents,
    AppendEvents {

    private val readJdbc = NamedParameterJdbcTemplate(dataSource)

    override fun load(streamId: StreamId): EventStream? = readJdbc.query(
        SqliteEventQueries.LOAD,
        mapOf("streamId" to streamId.value),
        ResultSetExtractor { rows -> readEventStream(streamId, rows, eventCodec) },
    )

    override fun append(
        streamId: StreamId,
        expectedRevision: StreamRevision,
        events: List<PendingEvent>,
    ): AppendResult {
        val batch = events.toList()
        require(batch.isNotEmpty()) { "An event append must not be empty" }
        val encoded = batch.map { EncodedPendingEvent(it.eventId, it.recordedAt, eventCodec.encode(it.payload)) }

        var committed: AppendResult.Committed? = null
        return try {
            dataSource.connection.use { connection ->
                appendBatch(connection, streamId, expectedRevision, encoded).also { result ->
                    if (result is AppendResult.Committed) committed = result
                }
            }
        } catch (failure: SQLException) {
            val result = committed ?: throw failure
            logger.warn("Could not close SQLite connection after committing stream {}", streamId.value, failure)
            result
        }
    }

    private fun appendBatch(
        connection: Connection,
        streamId: StreamId,
        expectedRevision: StreamRevision,
        batch: List<EncodedPendingEvent>,
    ): AppendResult {
        val position = runCatching {
            connection.autoCommit = false
            val jdbc = NamedParameterJdbcTemplate(SingleConnectionDataSource(connection, true))
            claim(jdbc, streamId, expectedRevision, batch.size)?.also {
                insertEvents(jdbc, streamId, it, batch)
                commit(connection, streamId)
            }
        }.onFailure { failure ->
            runCatching { connection.rollback() }.exceptionOrNull()?.let(failure::addSuppressed)
        }.getOrThrow()

        return if (position == null) {
            connection.rollback()
            AppendResult.RevisionMismatch
        } else {
            AppendResult.Committed(position.revision)
        }
    }

    private fun claim(
        jdbc: NamedParameterJdbcTemplate,
        streamId: StreamId,
        expectedRevision: StreamRevision,
        batchSize: Int,
    ): ClaimedPosition? {
        val creating = expectedRevision == StreamRevision.INITIAL
        val sql = if (creating) SqliteEventQueries.CREATE else SqliteEventQueries.ADVANCE
        return jdbc.query(
            sql,
            mapOf(
                "streamId" to streamId.value,
                "expectedRevision" to expectedRevision.value,
                "batchSize" to batchSize,
            ),
            ResultSetExtractor { rows ->
                if (rows.next()) {
                    ClaimedPosition(
                        StreamRevision(rows.getLong("revision")),
                        rows.getLong("last_sequence") - batchSize + 1,
                    )
                } else {
                    null
                }
            },
        )
    }

    private fun insertEvents(
        jdbc: NamedParameterJdbcTemplate,
        streamId: StreamId,
        position: ClaimedPosition,
        events: List<EncodedPendingEvent>,
    ) {
        for ((index, event) in events.withIndex()) {
            val affected = jdbc.update(
                SqliteEventQueries.INSERT,
                mapOf(
                    "eventId" to event.eventId.value.toString(),
                    "streamId" to streamId.value,
                    "sequence" to position.firstSequence + index,
                    "revision" to position.revision.value,
                    "recordedAt" to event.recordedAt.toString(),
                    "eventType" to event.encoded.type,
                    "eventVersion" to event.encoded.version,
                    "payload" to event.encoded.payload,
                ),
            )
            check(affected == 1) { "An event insert must persist exactly one row" }
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

    private class EncodedPendingEvent(val eventId: EventId, val recordedAt: Instant, val encoded: EncodedEvent)

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(SqliteEventStore::class.java)
    }
}

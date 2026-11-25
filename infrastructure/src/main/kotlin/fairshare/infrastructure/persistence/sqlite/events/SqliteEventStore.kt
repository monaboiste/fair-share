package fairshare.infrastructure.persistence.sqlite.events

import fairshare.application.commands.ports.AppendEvents
import fairshare.application.commands.ports.AppendResult
import fairshare.application.commands.ports.LoadEvents
import fairshare.domain.events.EventId
import fairshare.domain.events.EventStream
import fairshare.domain.events.PendingEvent
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import fairshare.infrastructure.persistence.sqlite.UnknownCommitOutcomeException
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.TransactionSystemException
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.time.Instant

/**
 * SQLite event persistence on unpooled connections. Each append owns its transaction and rejects
 * ambient transactions. Commit failures with uncertain durability throw [UnknownCommitOutcomeException].
 */
internal class SqliteEventStore(
    private val jdbc: NamedParameterJdbcTemplate,
    private val transactions: TransactionTemplate,
    private val eventCodec: EventCodec,
) : LoadEvents,
    AppendEvents {

    override fun load(streamId: StreamId): EventStream? {
        val parameters = mapOf("streamId" to streamId.value)
        return jdbc.query(
            SqliteEventQueries.LOAD,
            parameters,
            ResultSetExtractor { rows -> readEventStream(streamId, rows, eventCodec) },
        )
    }

    override fun append(
        streamId: StreamId,
        expectedRevision: StreamRevision,
        events: List<PendingEvent>,
    ): AppendResult {
        val batch = events.toList()
        require(batch.isNotEmpty()) { "An event append must not be empty" }
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "SQLite event append does not support an ambient transaction"
        }
        val encoded = batch.map { EncodedPendingEvent(it.eventId, it.recordedAt, eventCodec.encode(it.payload)) }

        val revision = try {
            transactions.execute { status ->
                check(status.isNewTransaction) { "SQLite event append requires a new physical transaction" }
                val position = claim(streamId, expectedRevision, encoded.size)
                if (position == null) {
                    status.setRollbackOnly()
                } else {
                    insertEvents(streamId, position, encoded)
                }
                position?.revision
            }
        } catch (rollback: TransactionSystemException) {
            val original = rollback.applicationException ?: throw rollback
            original.addSuppressed(rollback.cause ?: rollback)
            throw original
        }
        return revision?.let(AppendResult::Committed) ?: AppendResult.RevisionMismatch
    }

    /**
     * Reserves a contiguous sequence range at the next revision within the append transaction.
     * Returns `null` without changing stream metadata if the expected revision does not match.
     */
    private fun claim(streamId: StreamId, expectedRevision: StreamRevision, batchSize: Int): ClaimedPosition? {
        val creating = expectedRevision == StreamRevision.INITIAL
        val sql = if (creating) SqliteEventQueries.CREATE else SqliteEventQueries.ADVANCE
        val parameters = mapOf(
            "streamId" to streamId.value,
            "expectedRevision" to expectedRevision.value,
            "batchSize" to batchSize,
        )
        return jdbc.query(sql, parameters, ResultSetExtractor { rows -> readClaimedPosition(rows, batchSize) })
    }

    private fun readClaimedPosition(rows: ResultSet, batchSize: Int): ClaimedPosition? {
        if (!rows.next()) return null
        return ClaimedPosition(
            StreamRevision(rows.getLong("revision")),
            rows.getLong("last_sequence") - batchSize + 1,
        )
    }

    private fun insertEvents(streamId: StreamId, position: ClaimedPosition, events: List<EncodedPendingEvent>) {
        for ((index, event) in events.withIndex()) {
            val parameters = mapOf(
                "eventId" to event.eventId.value.toString(),
                "streamId" to streamId.value,
                "sequence" to position.firstSequence + index,
                "revision" to position.revision.value,
                "recordedAt" to event.recordedAt.toString(),
                "eventType" to event.encoded.type,
                "eventVersion" to event.encoded.version,
                "payload" to event.encoded.payload,
            )
            check(jdbc.update(SqliteEventQueries.INSERT, parameters) == 1) {
                "An event insert must persist exactly one row"
            }
        }
    }

    private class ClaimedPosition(val revision: StreamRevision, val firstSequence: Long)

    private class EncodedPendingEvent(val eventId: EventId, val recordedAt: Instant, val encoded: EncodedEvent)
}

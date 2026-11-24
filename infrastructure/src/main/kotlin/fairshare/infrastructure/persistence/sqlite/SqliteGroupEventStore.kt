package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupEventStore
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStoreUnavailable
import fairshare.application.group.GroupStream
import fairshare.domain.group.GroupId
import fairshare.domain.group.events.GroupCreated
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.time.Clock
import javax.sql.DataSource

public class SqliteGroupEventStore(private val dataSource: DataSource, private val clock: Clock) : GroupEventStore {
    override fun load(id: GroupId): GroupStream? = readGroupRows(dataSource, READ_EVENTS, id) { rows ->
        val decoder = GroupEventDecoder(id)
        val events = buildList {
            while (rows.next()) add(decoder.decode(rows).envelope(id))
        }
        events.lastOrNull()?.let { GroupStream(it.revision, events) }
    }

    override fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend {
        val events = batch.events
        require((expected == null) == (events.first() is GroupCreated)) {
            "Creation must be the first event of a new stream"
        }
        val revision = GroupRevision.random()
        val recordedAt = clock.instant()
        val encoded = events.map { EncodedEvent(it, revision, recordedAt) }
        val append = AppendTransaction(id, expected, revision)
        return try {
            useJdbc(dataSource.connection) { connection -> append.write(connection, encoded) }
        } catch (failure: SQLException) {
            append.reportFailure(failure)
        }
    }
}

private class AppendTransaction(
    private val id: GroupId,
    private val expected: GroupRevision?,
    private val revision: GroupRevision,
) {
    private var commit = CommitState.NOT_ATTEMPTED

    fun write(connection: Connection, events: List<EncodedEvent>): GroupAppend {
        connection.autoCommit = false
        val result = try {
            when (insertEvents(connection, id, expected, events)) {
                AppendDecision.INSERTED -> commit(connection)
                AppendDecision.STALE -> GroupAppend.RevisionMismatch
            }
        } catch (failure: SQLException) {
            rollbackAfterFailure(connection, failure)
            throw failure
        }
        if (result == GroupAppend.RevisionMismatch) connection.rollback()
        return result
    }

    fun reportFailure(failure: SQLException): GroupAppend {
        val context = SQLException("Group ${id.value} append from ${expected?.value} to ${revision.value}", failure)
        return when (commit) {
            CommitState.NOT_ATTEMPTED -> throw GroupStoreUnavailable(context)

            CommitState.IN_PROGRESS -> throw GroupAppendOutcomeUnknown(context)

            CommitState.COMMITTED -> {
                log.warn("Closing committed Group write failed", context)
                GroupAppend.Committed(revision)
            }
        }
    }

    private fun commit(connection: Connection): GroupAppend.Committed {
        commit = CommitState.IN_PROGRESS
        connection.commit()
        commit = CommitState.COMMITTED
        return GroupAppend.Committed(revision)
    }
}

private enum class CommitState { NOT_ATTEMPTED, IN_PROGRESS, COMMITTED }

private val log = LoggerFactory.getLogger(SqliteGroupEventStore::class.java)

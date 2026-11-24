package fairshare.infrastructure.persistence

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupEventBatch
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStore
import fairshare.application.group.GroupStoreUnavailable
import fairshare.application.group.GroupStream
import fairshare.domain.group.GroupCreated
import fairshare.domain.group.GroupEvent
import fairshare.domain.group.GroupId
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import java.util.UUID

public class SqliteGroupStore(private val settings: SqliteGroupStoreSettings) : GroupStore {
    private val connectionProperties = Properties().apply {
        setProperty("busy_timeout", settings.busyTimeoutMillis.toString())
        setProperty("synchronous", settings.synchronous.name)
    }

    init {
        readFromDatabase(Connection::migrateGroupSchema)
    }

    override fun load(id: GroupId): GroupStream? = readFromDatabase { connection ->
        val query = connection.prepareStatement(LOAD_STREAM)
        query.setString(1, id.value)
        query.executeQuery().readGroupStream()
    }

    override fun append(id: GroupId, expected: GroupRevision?, batch: GroupEventBatch): GroupAppend {
        val events = batch.events
        require((expected == null) == (events.first() is GroupCreated)) {
            "Creation must be the first event of a new stream"
        }
        val next = GroupRevision(UUID.randomUUID().toString())
        var commitStarted = false
        return try {
            withConnection { connection ->
                connection.autoCommit = false
                if (connection.appendAtRevision(id, expected, next, events)) {
                    commitStarted = true
                    connection.commit()
                    GroupAppend.Committed(next)
                } else {
                    GroupAppend.RevisionMismatch
                }
            }
        } catch (failure: SQLException) {
            throw if (commitStarted) GroupAppendOutcomeUnknown(failure) else GroupStoreUnavailable(failure)
        }
    }

    private fun <T> withConnection(operation: (Connection) -> T): T =
        DriverManager.getConnection(settings.jdbcUrl, connectionProperties).use(operation)

    private fun <T> readFromDatabase(read: (Connection) -> T): T = try {
        withConnection(read)
    } catch (failure: SQLException) {
        throw GroupStoreUnavailable(failure)
    }
}

private fun Connection.appendAtRevision(
    id: GroupId,
    expected: GroupRevision?,
    next: GroupRevision,
    events: List<GroupEvent>,
): Boolean {
    val command = prepareStatement(APPEND_AT_REVISION)
    command.bindAppend(id, expected, next, events.first())
    if (command.executeUpdate() != 1) return false

    for (event in events.drop(1)) {
        command.bindAppend(id, next, next, event)
        if (command.executeUpdate() != 1) throw SQLException("Group event batch append did not insert one row")
    }
    return true
}

internal val LOAD_STREAM = """
    SELECT revision, kind, format_version, display_name, currency
    FROM group_events WHERE group_id = ? ORDER BY position
""".trimIndent()

private val APPEND_AT_REVISION = """
    INSERT INTO group_events (group_id, position, revision, kind, format_version, display_name, currency)
    SELECT ?1, COALESCE(previous.position, 0) + 1, ?2, ?3, ?4, ?5, ?6
    FROM (SELECT 1)
    LEFT JOIN (
        SELECT position, revision FROM group_events WHERE group_id = ?1 ORDER BY position DESC LIMIT 1
    ) previous ON TRUE
    WHERE previous.revision IS ?7
""".trimIndent()

package fairshare.infrastructure.persistence

import fairshare.application.group.GroupAppend
import fairshare.application.group.GroupAppendOutcomeUnknown
import fairshare.application.group.GroupId
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStore
import fairshare.application.group.GroupStoreUnavailable
import fairshare.application.group.GroupStream
import fairshare.domain.group.GroupEvent
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import java.util.UUID

public class SqliteGroupStore(private val jdbcUrl: String) : GroupStore {
    private val settings = Properties().apply {
        setProperty("busy_timeout", "5000")
        setProperty("synchronous", "EXTRA")
    }

    init {
        readFromDatabase { connection ->
            connection.createStatement().use { statement -> statement.execute(CREATE_TABLE) }
        }
    }

    override fun load(id: GroupId): GroupStream? = readFromDatabase { connection ->
        connection.prepareStatement(LOAD_STREAM).use { query ->
            query.setString(1, id.value)
            query.executeQuery().use { it.readGroupStream() }
        }
    }

    override fun append(id: GroupId, expected: GroupRevision?, event: GroupEvent): GroupAppend {
        require((expected == null) == (event is GroupEvent.Created))
        val next = GroupRevision(UUID.randomUUID().toString())
        var commitStarted = false
        return try {
            openConnection().use { connection ->
                connection.autoCommit = false
                val inserted = connection.prepareStatement(APPEND_AT_REVISION).use { command ->
                    command.bindAppend(id, expected, next, event)
                    command.executeUpdate() == 1
                }
                if (inserted) {
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

    private fun openConnection(): Connection = DriverManager.getConnection(jdbcUrl, settings)

    private fun <T> readFromDatabase(read: (Connection) -> T): T = try {
        openConnection().use(read)
    } catch (failure: SQLException) {
        throw GroupStoreUnavailable(failure)
    }
}

private val CREATE_TABLE = """
    CREATE TABLE IF NOT EXISTS group_events (
        group_id TEXT NOT NULL,
        position INTEGER NOT NULL,
        revision TEXT NOT NULL,
        kind TEXT NOT NULL,
        display_name TEXT,
        currency TEXT NOT NULL,
        PRIMARY KEY (group_id, position)
    )
""".trimIndent()

private val LOAD_STREAM = """
    SELECT revision, kind, display_name, currency
    FROM group_events WHERE group_id = ? ORDER BY position
""".trimIndent()

private val APPEND_AT_REVISION = """
    INSERT INTO group_events (group_id, position, revision, kind, display_name, currency)
    SELECT ?, COALESCE(previous.position, 0) + 1, ?, ?, ?, ?
    FROM (SELECT 1)
    LEFT JOIN (
        SELECT position, revision FROM group_events WHERE group_id = ? ORDER BY position DESC LIMIT 1
    ) previous ON TRUE
    WHERE previous.revision IS ?
""".trimIndent()

package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupRevision
import fairshare.domain.group.GroupId
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

internal fun <T> readGroupRows(dataSource: DataSource, sql: String, id: GroupId, read: (ResultSet) -> T): T =
    readDatabase(dataSource) { connection ->
        useJdbc(connection.prepareStatement(sql)) { query ->
            query.setString(1, id.value)
            useJdbc(query.executeQuery(), read)
        }
    }

internal fun insertEvents(
    connection: Connection,
    id: GroupId,
    expected: GroupRevision?,
    events: List<EncodedEvent>,
): AppendDecision = useJdbc(connection.prepareStatement(INSERT_EVENT)) { command ->
    val first = events.first()
    command.setString(1, id.value)
    command.setString(2, expected?.value)
    first.bindTo(command)
    if (command.executeUpdate() != 1) {
        if (currentRevision(connection, id) != expected?.value) return@useJdbc AppendDecision.STALE
        throw SQLException("Group event batch did not insert its first row")
    }
    command.setString(2, first.revision.value)
    for (event in events.drop(1)) {
        event.bindTo(command)
        if (command.executeUpdate() != 1) throw SQLException("Group event batch did not insert a subsequent row")
    }
    AppendDecision.INSERTED
}

internal enum class AppendDecision { INSERTED, STALE }

private fun currentRevision(connection: Connection, id: GroupId): String? =
    useJdbc(connection.prepareStatement(READ_REVISION)) { query ->
        query.setString(1, id.value)
        useJdbc(query.executeQuery()) { rows -> if (rows.next()) rows.getString(1) else null }
    }

internal val READ_EVENTS = """
    SELECT position, event_id, revision, recorded_at, kind, format_version, display_name, currency
    FROM group_events WHERE group_id = ? ORDER BY position
""".trimIndent()

internal val READ_VIEW = """
    SELECT events.position, events.event_id, events.revision, events.recorded_at,
        events.kind, events.format_version, events.display_name, events.currency,
        origin.display_name AS group_name, head.currency AS settlement_currency, head.revision AS group_revision
    FROM group_events events
    CROSS JOIN (SELECT display_name FROM group_events WHERE group_id = ?1 ORDER BY position LIMIT 1) origin
    CROSS JOIN (SELECT currency, revision FROM group_events WHERE group_id = ?1 ORDER BY position DESC LIMIT 1) head
    WHERE events.group_id = ?1 ORDER BY events.position
""".trimIndent()

private val READ_REVISION = """
    SELECT revision FROM group_events WHERE group_id = ? ORDER BY position DESC LIMIT 1
""".trimIndent()

private val INSERT_EVENT = """
    INSERT INTO group_events (
        group_id, position, event_id, revision, recorded_at, kind, format_version, display_name, currency
    )
    SELECT ?1, COALESCE(previous.position, 0) + 1, ?3, ?4, ?5, ?6, ?7, ?8, ?9
    FROM (SELECT 1)
    LEFT JOIN (
        SELECT position, revision FROM group_events WHERE group_id = ?1 ORDER BY position DESC LIMIT 1
    ) previous ON TRUE
    WHERE previous.revision IS ?2
""".trimIndent()

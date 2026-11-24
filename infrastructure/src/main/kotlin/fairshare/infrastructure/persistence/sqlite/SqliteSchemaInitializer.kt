package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.GroupStoreUnavailable
import java.sql.SQLException
import javax.sql.DataSource

public class SqliteSchemaInitializer(private val dataSource: DataSource) {
    public fun initialize() {
        val compatible = readDatabase(dataSource) { connection ->
            useJdbc(connection.createStatement()) { command ->
                command.execute("CREATE TABLE IF NOT EXISTS $TABLE_DEFINITION")
                useJdbc(command.executeQuery(READ_SCHEMA)) { table ->
                    table.next() && table.getString(1) == "CREATE TABLE $TABLE_DEFINITION"
                }
            }
        }
        if (!compatible) throw GroupStoreUnavailable(SQLException("Incompatible Group event table"))
    }
}

private val TABLE_DEFINITION = """
    group_events (
        group_id TEXT NOT NULL,
        position INTEGER NOT NULL,
        event_id TEXT NOT NULL UNIQUE,
        revision TEXT NOT NULL,
        recorded_at TEXT NOT NULL,
        kind TEXT NOT NULL,
        format_version INTEGER NOT NULL,
        display_name TEXT,
        currency TEXT NOT NULL,
        PRIMARY KEY (group_id, position)
    ) STRICT
""".trimIndent()

private const val READ_SCHEMA = "SELECT sql FROM main.sqlite_schema WHERE type = 'table' AND name = 'group_events'"

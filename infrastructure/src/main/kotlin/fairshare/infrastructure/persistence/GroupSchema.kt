package fairshare.infrastructure.persistence

import java.sql.Connection

internal fun Connection.migrateGroupSchema() {
    if (needsMigration()) migrateLegacySchema()
    prepareStatement(LOAD_STREAM)
}

private fun Connection.migrateLegacySchema() {
    val migration = createStatement()
    migration.execute("BEGIN IMMEDIATE")
    if (needsMigration()) {
        migration.execute(LEGACY_TABLE)
        migration.execute(VERSIONED_TABLE)
        migration.executeUpdate(COPY_LEGACY_EVENTS)
        migration.execute("DROP TABLE group_events")
        migration.execute("ALTER TABLE group_events_v1 RENAME TO group_events")
        migration.execute("PRAGMA user_version = $CURRENT_SCHEMA")
    }
    migration.execute("COMMIT")
}

private fun Connection.needsMigration(): Boolean {
    val rows = createStatement().executeQuery("PRAGMA user_version")
    check(rows.next()) { "Group schema version missing" }
    val version = rows.getInt(1)
    rows.next()
    return when (version) {
        0 -> true
        CURRENT_SCHEMA -> false
        else -> error("Unsupported Group schema version: $version")
    }
}

private const val CURRENT_SCHEMA = 1

private val LEGACY_TABLE = """
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

private val VERSIONED_TABLE = """
    CREATE TABLE group_events_v1 (
        group_id TEXT NOT NULL,
        position INTEGER NOT NULL,
        revision TEXT NOT NULL,
        kind TEXT NOT NULL,
        format_version INTEGER NOT NULL,
        display_name TEXT,
        currency TEXT NOT NULL,
        PRIMARY KEY (group_id, position)
    )
""".trimIndent()

private val COPY_LEGACY_EVENTS = """
    INSERT INTO group_events_v1 (group_id, position, revision, kind, format_version, display_name, currency)
    SELECT group_id, position, revision, kind, 1, display_name, currency FROM group_events
""".trimIndent()

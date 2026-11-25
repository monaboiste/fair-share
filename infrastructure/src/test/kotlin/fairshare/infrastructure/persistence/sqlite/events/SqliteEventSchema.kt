package fairshare.infrastructure.persistence.sqlite.events

import javax.sql.DataSource

internal object SqliteEventSchema {
    fun initialize(dataSource: DataSource) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            connection.createStatement().use { statement ->
                statement.execute(CREATE_STREAMS)
                statement.execute(CREATE_EVENTS)
            }
            connection.commit()
        }
    }

    // language=sqlite
    private const val CREATE_STREAMS = """
        CREATE TABLE IF NOT EXISTS event_stream (
            stream_id TEXT NOT NULL PRIMARY KEY,
            revision INTEGER NOT NULL CHECK (revision > 0),
            last_sequence INTEGER NOT NULL CHECK (last_sequence >= revision)
        ) STRICT
    """

    // language=sqlite
    private const val CREATE_EVENTS = """
        CREATE TABLE IF NOT EXISTS event (
            event_id TEXT NOT NULL UNIQUE,
            stream_id TEXT NOT NULL REFERENCES event_stream (stream_id),
            sequence INTEGER NOT NULL CHECK (sequence > 0),
            revision INTEGER NOT NULL CHECK (revision > 0 AND revision <= sequence),
            recorded_at TEXT NOT NULL,
            event_type TEXT NOT NULL CHECK (length(event_type) > 0),
            event_version INTEGER NOT NULL CHECK (event_version BETWEEN 1 AND 2147483647),
            payload TEXT NOT NULL,
            PRIMARY KEY (stream_id, sequence)
        ) STRICT
    """
}

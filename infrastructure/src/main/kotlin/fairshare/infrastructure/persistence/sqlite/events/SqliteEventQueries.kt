package fairshare.infrastructure.persistence.sqlite.events

internal object SqliteEventQueries {
    // language=sqlite
    const val LOAD = """
        SELECT s.revision AS stream_revision, s.last_sequence,
               e.event_id, e.stream_id, e.sequence, e.revision, e.recorded_at,
               e.event_type, e.event_version, e.payload
        FROM (SELECT ? AS stream_id) AS requested
        LEFT JOIN event_stream s ON s.stream_id = requested.stream_id
        LEFT JOIN event e ON e.stream_id = requested.stream_id
        WHERE s.stream_id IS NOT NULL OR e.stream_id IS NOT NULL
        ORDER BY e.sequence
    """

    // language=sqlite
    const val CREATE = """
        INSERT INTO event_stream (stream_id, revision, last_sequence)
        VALUES (?, 1, ?)
        ON CONFLICT (stream_id) DO NOTHING
        RETURNING revision, last_sequence
    """

    // language=sqlite
    const val ADVANCE = """
        UPDATE event_stream
        SET revision = revision + 1, last_sequence = last_sequence + ?
        WHERE stream_id = ? AND revision = ?
        RETURNING revision, last_sequence
    """

    // language=sqlite
    const val INSERT = """
        INSERT INTO event (
            event_id, stream_id, sequence, revision, recorded_at, event_type, event_version, payload
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """
}

package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.EventEnvelope
import fairshare.domain.events.EventId
import fairshare.domain.events.EventStream
import fairshare.domain.events.StreamId
import fairshare.domain.events.StreamRevision
import java.sql.ResultSet
import java.time.DateTimeException
import java.time.Instant
import java.util.UUID

internal fun readEventStream(streamId: StreamId, rows: ResultSet, codec: EventCodec): EventStream? {
    if (!rows.next()) return null

    try {
        val revision = StreamRevision(rows.getLong("stream_revision"))
        val lastSequence = rows.getLong("last_sequence")
        val events = mutableListOf<EventEnvelope>()
        do {
            val eventId = rows.getString("event_id")
            if (eventId != null) events += readEvent(eventId, rows, codec)
        } while (rows.next())

        val stream = EventStream(streamId, revision, events)
        require(stream.events.last().sequence == lastSequence) {
            "Stream metadata does not match the final event sequence"
        }
        return stream
    } catch (failure: IllegalArgumentException) {
        throw InvalidEventHistoryException("Invalid history for stream ${streamId.value}", failure)
    } catch (failure: DateTimeException) {
        throw InvalidEventHistoryException("Invalid timestamp in stream ${streamId.value}", failure)
    }
}

private fun readEvent(eventId: String, rows: ResultSet, codec: EventCodec): EventEnvelope {
    val uuid = UUID.fromString(eventId)
    require(uuid.toString() == eventId) { "Event ID is not a canonical UUID" }
    val version = rows.getInt("event_version")
    require(version.toLong() == rows.getLong("event_version")) { "Event format version is out of range" }
    val encoded = EncodedEvent(
        type = rows.getString("event_type"),
        version = version,
        payload = rows.getString("payload"),
    )
    return EventEnvelope(
        eventId = EventId(uuid),
        streamId = StreamId(rows.getString("stream_id")),
        sequence = rows.getLong("sequence"),
        revision = StreamRevision(rows.getLong("revision")),
        recordedAt = Instant.parse(rows.getString("recorded_at")),
        payload = codec.decode(encoded),
    )
}

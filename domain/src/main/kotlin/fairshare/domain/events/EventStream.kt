package fairshare.domain.events

import java.util.Collections

public class EventStream(
    public val streamId: StreamId,
    public val revision: StreamRevision,
    events: List<EventEnvelope>,
) {
    public val events: List<EventEnvelope> = Collections.unmodifiableList(events.toList())

    init {
        require(this.events.isNotEmpty()) { "Recorded stream must contain events" }
        require(this.events.first().revision.value == 1L) { "Recorded stream must start at revision one" }

        val eventIds = mutableSetOf<EventId>()
        var previousRevision = 1L
        for (index in this.events.indices) {
            val event = this.events[index]
            require(event.streamId == streamId) { "Event belongs to a different stream" }
            require(event.sequence == index + 1L) { "Event sequences must be contiguous and in order" }
            require(eventIds.add(event.eventId)) { "Event IDs must be unique within the stream" }

            val currentRevision = event.revision.value
            require(currentRevision - previousRevision in 0L..1L) {
                "Event revision must stay the same or advance by one"
            }
            previousRevision = currentRevision
        }

        require(previousRevision == revision.value) { "Stream revision must match the final event revision" }
    }
}

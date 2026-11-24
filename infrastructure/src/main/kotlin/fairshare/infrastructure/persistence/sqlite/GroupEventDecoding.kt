package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.EventId
import fairshare.application.group.GroupEventEnvelope
import fairshare.application.group.GroupRevision
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.events.GroupEvent
import fairshare.domain.group.events.GroupSettlementCurrencyChanged
import fairshare.domain.group.exception.InvalidGroupNameException
import fairshare.domain.group.exception.InvalidSettlementCurrencyException
import java.sql.ResultSet
import java.time.Instant
import java.time.format.DateTimeParseException

internal data class DecodedEvent(
    val eventId: EventId,
    val sequence: Long,
    val revision: GroupRevision,
    val recordedAt: Instant,
    val event: GroupEvent,
) {
    fun envelope(groupId: GroupId): GroupEventEnvelope =
        GroupEventEnvelope(eventId, groupId, sequence, revision, recordedAt, event)
}

internal class GroupEventDecoder(private val id: GroupId) {
    private val revisions = mutableSetOf<GroupRevision>()
    private var previousRevision: GroupRevision? = null
    private var position = 0L

    fun decode(rows: ResultSet): DecodedEvent {
        val eventId = requiredText(rows, "event_id", eventId = null)
        val event = decodeRow(rows, eventId)
        position++
        valid(event.sequence == position, eventId, "Non-contiguous sequence")
        valid((position == 1L) == (event.event is GroupCreated), eventId, "Invalid creation position")
        if (event.revision != previousRevision) {
            valid(revisions.add(event.revision), eventId, "Revision reused in later batch")
            previousRevision = event.revision
        }
        return event
    }

    private fun decodeRow(rows: ResultSet, eventId: String): DecodedEvent = try {
        val revision = requiredText(rows, "revision", eventId)
        valid(eventId.isNotBlank(), eventId, "Missing event ID")
        valid(revision.isNotBlank(), eventId, "Missing revision")
        DecodedEvent(
            eventId = EventId(eventId),
            sequence = rows.getLong("position"),
            revision = GroupRevision(revision),
            recordedAt = Instant.parse(requiredText(rows, "recorded_at", eventId)),
            event = decodePayload(rows, eventId),
        )
    } catch (invalid: InvalidGroupNameException) {
        invalidHistory(eventId, "Invalid Group name", invalid)
    } catch (invalid: InvalidSettlementCurrencyException) {
        invalidHistory(eventId, "Invalid settlement currency", invalid)
    } catch (invalid: DateTimeParseException) {
        invalidHistory(eventId, "Invalid timestamp", invalid)
    }

    private fun decodePayload(rows: ResultSet, eventId: String): GroupEvent {
        val version = rows.getLong("format_version")
        valid(version == EVENT_FORMAT_V1.toLong(), eventId, "Unsupported format version: $version")
        val storedKind = requiredText(rows, "kind", eventId)
        val kind = GroupEventKind.fromStoredValue(storedKind)
            ?: invalidHistory(eventId, "Unsupported event kind: $storedKind")
        val currency = SettlementCurrency(requiredText(rows, "currency", eventId))
        val name = rows.getString("display_name")
        return when (kind) {
            GroupEventKind.CREATED -> GroupCreated(
                GroupName(name ?: invalidHistory(eventId, "Missing Group name")),
                currency,
            )

            GroupEventKind.CURRENCY_CHANGED -> {
                valid(name == null, eventId, "Unexpected name on settlement currency change")
                GroupSettlementCurrencyChanged(currency)
            }
        }
    }

    private fun valid(condition: Boolean, eventId: String, detail: String) {
        if (!condition) invalidHistory(eventId, detail)
    }

    private fun requiredText(rows: ResultSet, column: String, eventId: String?): String =
        rows.getString(column) ?: invalidHistory(eventId, "Missing $column")

    private fun invalidHistory(eventId: String?, detail: String, cause: Throwable? = null): Nothing =
        throw IllegalStateException("Invalid Group ${id.value} history at event '$eventId': $detail", cause)
}

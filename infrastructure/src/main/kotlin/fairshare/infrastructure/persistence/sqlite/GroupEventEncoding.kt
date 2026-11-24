package fairshare.infrastructure.persistence.sqlite

import fairshare.application.group.EventId
import fairshare.application.group.GroupRevision
import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.events.GroupEvent
import fairshare.domain.group.events.GroupSettlementCurrencyChanged
import java.sql.PreparedStatement
import java.time.Instant

internal enum class GroupEventKind(val storedValue: String) {
    CREATED("group-created"),
    CURRENCY_CHANGED("settlement-currency-changed"),
    ;

    companion object {
        fun fromStoredValue(value: String): GroupEventKind? = entries.firstOrNull { it.storedValue == value }
    }
}

internal const val EVENT_FORMAT_V1 = 1

internal class EncodedEvent(event: GroupEvent, val revision: GroupRevision, private val recordedAt: Instant) {
    private val eventId = EventId.random()

    private val kind = when (event) {
        is GroupCreated -> GroupEventKind.CREATED
        is GroupSettlementCurrencyChanged -> GroupEventKind.CURRENCY_CHANGED
    }

    private val displayName = (event as? GroupCreated)?.name?.value
    private val currency = when (event) {
        is GroupCreated -> event.currency
        is GroupSettlementCurrencyChanged -> event.currency
    }

    fun bindTo(command: PreparedStatement) {
        with(command) {
            setString(EVENT_ID, eventId.value)
            setString(REVISION, revision.value)
            setString(RECORDED_AT, recordedAt.toString())
            setString(KIND, kind.storedValue)
            setInt(FORMAT, EVENT_FORMAT_V1)
            setString(DISPLAY_NAME, displayName)
            setString(CURRENCY, currency.code)
        }
    }
}

private const val EVENT_ID = 3
private const val REVISION = 4
private const val RECORDED_AT = 5
private const val KIND = 6
private const val FORMAT = 7
private const val DISPLAY_NAME = 8
private const val CURRENCY = 9

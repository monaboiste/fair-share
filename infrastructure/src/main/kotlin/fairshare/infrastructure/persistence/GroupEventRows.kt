package fairshare.infrastructure.persistence

import fairshare.application.group.GroupId
import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStream
import fairshare.domain.group.GroupEvent
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import java.sql.PreparedStatement
import java.sql.ResultSet

internal fun ResultSet.readGroupStream(): GroupStream? {
    val events = mutableListOf<GroupEvent>()
    var revision: GroupRevision? = null
    while (next()) {
        events += readEvent()
        revision = GroupRevision(getString("revision"))
    }
    return revision?.let { GroupStream(it, events.toList()) }
}

private fun ResultSet.readEvent(): GroupEvent {
    val currency = checkNotNull(SettlementCurrency.parse(getString("currency")))
    return when (getString("kind")) {
        CREATED -> GroupEvent.Created(checkNotNull(GroupName.parse(getString("display_name"))), currency)
        CURRENCY_CHANGED -> GroupEvent.CurrencyChanged(currency)
        else -> error("Unsupported Group event")
    }
}

internal fun PreparedStatement.bindAppend(
    id: GroupId,
    expected: GroupRevision?,
    next: GroupRevision,
    event: GroupEvent,
) {
    val (kind, name, currency) = when (event) {
        is GroupEvent.Created -> Triple(CREATED, event.name.value, event.currency.code)
        is GroupEvent.CurrencyChanged -> Triple(CURRENCY_CHANGED, null, event.currency.code)
    }
    listOf(id.value, next.value, kind, name, currency, id.value, expected?.value).forEachIndexed { index, value ->
        setString(index + 1, value)
    }
}

private const val CREATED = "group-created"
private const val CURRENCY_CHANGED = "settlement-currency-changed"

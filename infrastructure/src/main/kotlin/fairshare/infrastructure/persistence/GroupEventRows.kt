package fairshare.infrastructure.persistence

import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupStream
import fairshare.domain.group.GroupCreated
import fairshare.domain.group.GroupEvent
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.GroupSettlementCurrencyChanged
import fairshare.domain.group.SettlementCurrency
import java.sql.PreparedStatement
import java.sql.ResultSet

internal fun ResultSet.readGroupStream(): GroupStream? {
    val events = mutableListOf<GroupEvent>()
    var revision: GroupRevision? = null
    while (next()) {
        events += readEvent()
        revision = GroupRevision(requiredText("revision"))
    }
    return revision?.let { GroupStream(it, events.toList()) }
}

private fun ResultSet.readEvent(): GroupEvent {
    val version = getString("format_version")
    check(version == EVENT_FORMAT_V1.toString()) { "Unsupported Group event format version: $version" }
    val currency = checkNotNull(SettlementCurrency.parse(requiredText("currency"))) { "Invalid recorded currency" }
    return when (getString("kind")) {
        CREATED -> GroupCreated(
            checkNotNull(GroupName.parse(requiredText("display_name"))) { "Invalid recorded Group name" },
            currency,
        )

        CURRENCY_CHANGED -> GroupSettlementCurrencyChanged(currency)

        else -> error("Unsupported Group event")
    }
}

private fun ResultSet.requiredText(column: String): String =
    getString(column) ?: error("Missing Group event value: $column")

internal fun PreparedStatement.bindAppend(
    id: GroupId,
    expected: GroupRevision?,
    next: GroupRevision,
    event: GroupEvent,
) {
    setString(GROUP_ID, id.value)
    setString(NEXT_REVISION, next.value)
    setInt(FORMAT, EVENT_FORMAT_V1)
    setString(EXPECTED_REVISION, expected?.value)
    when (event) {
        is GroupCreated -> {
            setString(KIND, CREATED)
            setString(NAME, event.name.value)
            setString(CURRENCY, event.currency.code)
        }

        is GroupSettlementCurrencyChanged -> {
            setString(KIND, CURRENCY_CHANGED)
            setString(NAME, null)
            setString(CURRENCY, event.currency.code)
        }
    }
}

private const val GROUP_ID = 1
private const val NEXT_REVISION = 2
private const val KIND = 3
private const val FORMAT = 4
private const val NAME = 5
private const val CURRENCY = 6
private const val EXPECTED_REVISION = 7

private const val EVENT_FORMAT_V1 = 1
private const val CREATED = "group-created"
private const val CURRENCY_CHANGED = "settlement-currency-changed"

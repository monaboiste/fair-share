package fairshare.domain.group.events

import fairshare.domain.group.SettlementCurrency

public data class GroupSettlementCurrencyChanged(public val currency: SettlementCurrency) : GroupEvent

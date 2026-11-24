package fairshare.domain.group

public data class GroupCreated(public val name: GroupName, public val currency: SettlementCurrency) : GroupEvent

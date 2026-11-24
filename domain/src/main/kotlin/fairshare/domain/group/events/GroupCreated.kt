package fairshare.domain.group.events

import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency

public data class GroupCreated(public val name: GroupName, public val currency: SettlementCurrency) : GroupEvent

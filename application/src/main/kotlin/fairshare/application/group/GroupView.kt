package fairshare.application.group

import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency

public data class GroupView(
    public val id: GroupId,
    public val name: GroupName,
    public val settlementCurrency: SettlementCurrency,
    public val revision: GroupRevision,
)

package fairshare.domain.group

import fairshare.domain.group.events.GroupCreated
import fairshare.domain.group.events.GroupEvent
import fairshare.domain.group.events.GroupSettlementCurrencyChanged

public class Group private constructor(public val name: GroupName, public val settlementCurrency: SettlementCurrency) {
    public fun changeSettlementCurrency(currency: SettlementCurrency): GroupSettlementCurrencyChanged =
        GroupSettlementCurrencyChanged(currency)

    public fun apply(event: GroupSettlementCurrencyChanged): Group = Group(name, event.currency)

    public companion object {
        public fun create(name: GroupName, currency: SettlementCurrency): GroupCreated = GroupCreated(name, currency)

        public fun replay(events: List<GroupEvent>): Group {
            val created = checkNotNull(events.firstOrNull() as? GroupCreated) {
                "Group history must start with creation"
            }
            return events.drop(1).fold(Group(created.name, created.currency)) { group, event ->
                when (event) {
                    is GroupCreated -> error("Group history cannot contain a second creation")
                    is GroupSettlementCurrencyChanged -> group.apply(event)
                }
            }
        }
    }
}

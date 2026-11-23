package fairshare.domain.group

public sealed interface GroupEvent {
    public data class Created(public val name: GroupName, public val currency: SettlementCurrency) : GroupEvent
    public data class CurrencyChanged(public val currency: SettlementCurrency) : GroupEvent
}

public class Group private constructor(public val name: GroupName, public val settlementCurrency: SettlementCurrency) {
    public fun changeCurrency(currency: SettlementCurrency): GroupEvent.CurrencyChanged =
        GroupEvent.CurrencyChanged(currency)

    public fun apply(event: GroupEvent.CurrencyChanged): Group = Group(name, event.currency)

    public companion object {
        public fun replay(events: List<GroupEvent>): Group {
            val created = checkNotNull(events.firstOrNull() as? GroupEvent.Created) {
                "Group history must start with creation"
            }
            return events.drop(1).fold(Group(created.name, created.currency)) { group, event ->
                when (event) {
                    is GroupEvent.Created -> error("Group history cannot contain a second creation")
                    is GroupEvent.CurrencyChanged -> group.apply(event)
                }
            }
        }
    }
}

package fairshare.application.group

import fairshare.domain.group.Group
import fairshare.domain.group.GroupEvent
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import java.util.UUID

public data class GroupSnapshot(public val id: GroupId, public val revision: GroupRevision, public val group: Group)

public class GroupUseCases(private val store: GroupStore) {
    public fun create(displayName: String, settlementCurrency: String): GroupSnapshot {
        val name = GroupName.parse(displayName)
        val currency = SettlementCurrency.parse(settlementCurrency)
        if (name == null || currency == null) {
            throw GroupFailure.InvalidGroup(
                buildList {
                    if (name == null) add(GroupInputViolation.BLANK_DISPLAY_NAME)
                    if (currency == null) add(GroupInputViolation.INVALID_SETTLEMENT_CURRENCY)
                },
            )
        }
        val id = GroupId(UUID.randomUUID().toString())
        val event = GroupEvent.Created(name, currency)
        return GroupSnapshot(id, append(id, null, event), Group.replay(listOf(event)))
    }

    public fun get(id: GroupId): GroupSnapshot {
        val stream = store.load(id) ?: throw GroupFailure.NotFound()
        return GroupSnapshot(id, stream.revision, Group.replay(stream.events))
    }

    public fun changeCurrency(id: GroupId, expected: GroupRevision, settlementCurrency: String): GroupSnapshot {
        val current = get(id)
        if (current.revision != expected) throw GroupFailure.RevisionMismatch()
        val currency = SettlementCurrency.parse(settlementCurrency)
            ?: throw GroupFailure.InvalidGroup(listOf(GroupInputViolation.INVALID_SETTLEMENT_CURRENCY))
        val event = current.group.changeCurrency(currency)
        return GroupSnapshot(id, append(id, expected, event), current.group.apply(event))
    }

    private fun append(id: GroupId, expected: GroupRevision?, event: GroupEvent): GroupRevision =
        when (val result = store.append(id, expected, event)) {
            is GroupAppend.Committed -> result.revision

            GroupAppend.RevisionMismatch -> {
                checkNotNull(expected) { "Generated Group identifier already exists" }
                throw GroupFailure.RevisionMismatch()
            }
        }
}

package fairshare.application.group

import fairshare.domain.group.GroupId
import fairshare.domain.group.SettlementCurrency

public class ChangeGroupSettlementCurrencyUseCase(private val store: GroupStore) {
    private val getGroup = GetGroupUseCase(store)

    public fun execute(
        id: GroupId,
        precondition: GroupRevisionPrecondition,
        settlementCurrency: String,
    ): GroupSnapshot {
        val current = getGroup.execute(id)
        precondition.verify(current.revision)
        val currency = SettlementCurrency.parse(settlementCurrency)
            ?: throw GroupFailure.InvalidGroup(listOf(GroupInputViolation.INVALID_SETTLEMENT_CURRENCY))
        val event = current.group.changeSettlementCurrency(currency)
        val revision = when (val result = store.append(id, precondition.expected, GroupEventBatch(listOf(event)))) {
            is GroupAppend.Committed -> result.revision
            GroupAppend.RevisionMismatch -> throw GroupFailure.RevisionMismatch()
        }
        return GroupSnapshot(id, revision, current.group.apply(event))
    }
}

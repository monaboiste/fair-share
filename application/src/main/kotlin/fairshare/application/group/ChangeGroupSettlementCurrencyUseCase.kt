package fairshare.application.group

import fairshare.domain.group.Group
import fairshare.domain.group.GroupId
import fairshare.domain.group.SettlementCurrency

public class ChangeGroupSettlementCurrencyUseCase(private val store: GroupEventStore) {
    public fun execute(id: GroupId, precondition: GroupRevisionPrecondition, settlementCurrency: String): GroupView {
        val stream = store.load(id) ?: throw GroupFailure.NotFound()
        precondition.verify(stream.revision)
        val group = Group.replay(stream.events.map(GroupEventEnvelope::event))
        val event = group.changeSettlementCurrency(SettlementCurrency(settlementCurrency))
        val revision = when (val result = store.append(id, precondition.expected, GroupEventBatch(listOf(event)))) {
            is GroupAppend.Committed -> result.revision
            GroupAppend.RevisionMismatch -> throw GroupFailure.RevisionMismatch()
        }
        val updated = group.apply(event)
        return GroupView(id, updated.name, updated.settlementCurrency, revision)
    }
}

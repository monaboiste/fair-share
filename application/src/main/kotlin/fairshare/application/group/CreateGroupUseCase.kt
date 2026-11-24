package fairshare.application.group

import fairshare.domain.group.Group
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency

public class CreateGroupUseCase(private val store: GroupEventStore) {
    public fun execute(displayName: String, settlementCurrency: String): GroupView {
        val event = Group.create(GroupName(displayName), SettlementCurrency(settlementCurrency))
        val id = GroupId.random()
        val revision = when (val result = store.append(id, null, GroupEventBatch(listOf(event)))) {
            is GroupAppend.Committed -> result.revision
            GroupAppend.RevisionMismatch -> error("Generated Group identifier already exists")
        }
        return GroupView(id, event.name, event.currency, revision)
    }
}

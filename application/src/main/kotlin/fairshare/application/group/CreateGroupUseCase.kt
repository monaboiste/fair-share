package fairshare.application.group

import fairshare.domain.group.Group
import fairshare.domain.group.GroupId
import fairshare.domain.group.GroupName
import fairshare.domain.group.SettlementCurrency
import java.util.UUID

public class CreateGroupUseCase(private val store: GroupStore) {
    public fun execute(displayName: String, settlementCurrency: String): GroupSnapshot {
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
        val events = listOf(Group.create(name, currency))
        val revision = when (val result = store.append(id, null, GroupEventBatch(events))) {
            is GroupAppend.Committed -> result.revision
            GroupAppend.RevisionMismatch -> error("Generated Group identifier already exists")
        }
        return GroupSnapshot(id, revision, Group.replay(events))
    }
}

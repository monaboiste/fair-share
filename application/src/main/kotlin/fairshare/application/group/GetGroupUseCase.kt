package fairshare.application.group

import fairshare.domain.group.Group
import fairshare.domain.group.GroupId

public class GetGroupUseCase(private val store: GroupStore) {
    public fun execute(id: GroupId): GroupSnapshot {
        val stream = store.load(id) ?: throw GroupFailure.NotFound()
        return GroupSnapshot(id, stream.revision, Group.replay(stream.events))
    }
}

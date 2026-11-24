package fairshare.application.group

import fairshare.domain.group.GroupId

public interface GroupViewRepository {
    public fun findById(id: GroupId): GroupView?
}

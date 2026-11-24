package fairshare.application.group

import fairshare.domain.group.GroupId

public class GetGroupUseCase(private val repository: GroupViewRepository) {
    public fun execute(id: GroupId): GroupView = repository.findById(id) ?: throw GroupFailure.NotFound()
}

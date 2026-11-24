package fairshare.infrastructure.rest

import fairshare.application.group.ChangeGroupSettlementCurrencyUseCase
import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.domain.group.GroupId
import fairshare.infrastructure.rest.api.GroupApi
import fairshare.infrastructure.rest.model.GroupResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
internal class GroupController(
    private val createGroup: CreateGroupUseCase,
    private val getGroup: GetGroupUseCase,
    private val changeGroupSettlementCurrency: ChangeGroupSettlementCurrencyUseCase,
    private val requests: GroupRequestReader,
) : GroupApi {
    override fun create(body: ByteArray?): ResponseEntity<GroupResponse> {
        val request = requests.create(body)
        val created = createGroup.execute(request.displayName, request.settlementCurrency)
        return GroupResponses.created(created)
    }

    override fun get(groupId: String): ResponseEntity<GroupResponse> {
        val group = getGroup.execute(GroupId(groupId))
        return GroupResponses.current(group)
    }

    override fun changeCurrency(
        groupId: String,
        headers: HttpHeaders,
        body: ByteArray?,
    ): ResponseEntity<GroupResponse> {
        val precondition = groupRevisionPrecondition(headers)
        val request = requests.changeCurrency(body)
        val changed = changeGroupSettlementCurrency.execute(GroupId(groupId), precondition, request.settlementCurrency)
        return GroupResponses.current(changed)
    }
}

package fairshare.infrastructure.rest.api

import fairshare.application.group.GroupRevisionPrecondition
import fairshare.infrastructure.rest.model.ChangeGroupSettlementCurrencyRequest
import fairshare.infrastructure.rest.model.CreateGroupRequest
import fairshare.infrastructure.rest.model.GetGroupResponse
import fairshare.infrastructure.rest.model.GroupCreatedResponse
import fairshare.infrastructure.rest.model.GroupSettlementCurrencyChangedResponse
import org.springframework.http.ResponseEntity

internal interface GroupApi {
    fun create(request: CreateGroupRequest?): ResponseEntity<GroupCreatedResponse>

    fun get(groupId: String): ResponseEntity<GetGroupResponse>

    fun changeCurrency(
        groupId: String,
        precondition: GroupRevisionPrecondition,
        request: ChangeGroupSettlementCurrencyRequest?,
    ): ResponseEntity<GroupSettlementCurrencyChangedResponse>
}

package fairshare.infrastructure.rest

import fairshare.application.group.ChangeGroupSettlementCurrencyUseCase
import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.application.group.GroupRevisionPrecondition
import fairshare.domain.group.GroupId
import fairshare.infrastructure.rest.api.GroupApi
import fairshare.infrastructure.rest.model.ChangeGroupSettlementCurrencyRequest
import fairshare.infrastructure.rest.model.CreateGroupRequest
import fairshare.infrastructure.rest.model.GetGroupResponse
import fairshare.infrastructure.rest.model.GroupCreatedResponse
import fairshare.infrastructure.rest.model.GroupSettlementCurrencyChangedResponse
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/groups", produces = [APPLICATION_JSON_VALUE])
internal class GroupController(
    private val createGroup: CreateGroupUseCase,
    private val getGroup: GetGroupUseCase,
    private val changeGroupSettlementCurrency: ChangeGroupSettlementCurrencyUseCase,
) : GroupApi {
    @PostMapping(consumes = [APPLICATION_JSON_VALUE])
    override fun create(
        @Valid @RequestBody(required = false) request: CreateGroupRequest?,
    ): ResponseEntity<GroupCreatedResponse> {
        val body = request.requireJsonObject()
        val group = createGroup.execute(checkNotNull(body.displayName), checkNotNull(body.settlementCurrency))
        val response = GroupCreatedResponse(group.id.value, group.name.value, group.settlementCurrency.code)
        val headers = groupHeaders(group.revision.value)
        headers.location = URI.create("/groups/${group.id.value}")
        return ResponseEntity(response, headers, HttpStatus.CREATED)
    }

    @GetMapping("/{groupId}")
    override fun get(@PathVariable groupId: String): ResponseEntity<GetGroupResponse> {
        val group = getGroup.execute(GroupId(groupId))
        val response = GetGroupResponse(group.id.value, group.name.value, group.settlementCurrency.code)
        return ResponseEntity(response, groupHeaders(group.revision.value), HttpStatus.OK)
    }

    @PatchMapping("/{groupId}/currency", consumes = [APPLICATION_JSON_VALUE])
    override fun changeCurrency(
        @PathVariable groupId: String,
        @RequestAttribute(GROUP_PRECONDITION) precondition: GroupRevisionPrecondition,
        @Valid @RequestBody(required = false) request: ChangeGroupSettlementCurrencyRequest?,
    ): ResponseEntity<GroupSettlementCurrencyChangedResponse> {
        val body = request.requireJsonObject()
        val group = changeGroupSettlementCurrency.execute(
            GroupId(groupId),
            precondition,
            checkNotNull(body.settlementCurrency),
        )
        val response =
            GroupSettlementCurrencyChangedResponse(group.id.value, group.name.value, group.settlementCurrency.code)
        return ResponseEntity(response, groupHeaders(group.revision.value), HttpStatus.OK)
    }
}

private fun groupHeaders(revision: String): HttpHeaders = HttpHeaders().apply {
    eTag = "\"$revision\""
    setCacheControl(CacheControl.noStore())
}

private fun <T : Any> T?.requireJsonObject(): T =
    this ?: throw GroupRequestFailure(listOf(FieldCode.INVALID_TYPE.at("#")))

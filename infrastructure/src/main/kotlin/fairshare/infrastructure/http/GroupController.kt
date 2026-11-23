package fairshare.infrastructure.http

import fairshare.application.group.GroupId
import fairshare.application.group.GroupSnapshot
import fairshare.application.group.GroupUseCases
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/groups", produces = [APPLICATION_JSON_VALUE])
internal class GroupController(private val groups: GroupUseCases) {
    @PostMapping(consumes = [APPLICATION_JSON_VALUE])
    fun create(@RequestBody(required = false) body: ByteArray?): ResponseEntity<GroupResponse> {
        val request = createGroupRequest(body)
        val created = groups.create(request.displayName, request.settlementCurrency)
        return ResponseEntity.created(URI.create("/groups/${created.id.value}")).withGroup(created)
    }

    @GetMapping("/{groupId}")
    fun get(@PathVariable groupId: String): ResponseEntity<GroupResponse> =
        ResponseEntity.ok().withGroup(groups.get(GroupId(groupId)))

    @PatchMapping("/{groupId}/currency", consumes = [APPLICATION_JSON_VALUE])
    fun changeCurrency(
        @PathVariable groupId: String,
        @RequestHeader headers: HttpHeaders,
        @RequestBody(required = false) body: ByteArray?,
    ): ResponseEntity<GroupResponse> {
        val revision = groupRevision(headers)
        val currency = changeCurrencyRequest(body)
        return ResponseEntity.ok().withGroup(groups.changeCurrency(GroupId(groupId), revision, currency))
    }
}

internal data class GroupResponse(val groupId: String, val displayName: String, val settlementCurrency: String) {
    val settlementCurrencyLocked: Boolean = false
    val participants: List<Nothing> = emptyList()
}

private fun ResponseEntity.BodyBuilder.withGroup(snapshot: GroupSnapshot): ResponseEntity<GroupResponse> =
    cacheControl(CacheControl.noStore()).eTag(""""${snapshot.revision.value}"""").body(
        GroupResponse(snapshot.id.value, snapshot.group.name.value, snapshot.group.settlementCurrency.code),
    )

package fairshare.infrastructure.rest.api

import fairshare.infrastructure.rest.model.GroupResponse
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

@RequestMapping("/groups", produces = [APPLICATION_JSON_VALUE])
internal interface GroupApi {
    @PostMapping(consumes = [APPLICATION_JSON_VALUE])
    fun create(@RequestBody(required = false) body: ByteArray?): ResponseEntity<GroupResponse>

    @GetMapping("/{groupId}")
    fun get(@PathVariable("groupId") groupId: String): ResponseEntity<GroupResponse>

    @PatchMapping("/{groupId}/currency", consumes = [APPLICATION_JSON_VALUE])
    fun changeCurrency(
        @PathVariable("groupId") groupId: String,
        @RequestHeader headers: HttpHeaders,
        @RequestBody(required = false) body: ByteArray?,
    ): ResponseEntity<GroupResponse>
}

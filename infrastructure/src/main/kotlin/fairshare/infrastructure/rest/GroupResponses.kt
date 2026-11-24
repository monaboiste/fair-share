package fairshare.infrastructure.rest

import fairshare.application.group.GroupSnapshot
import fairshare.infrastructure.rest.model.GroupResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

internal object GroupResponses {
    fun created(snapshot: GroupSnapshot): ResponseEntity<GroupResponse> =
        response(snapshot, HttpStatus.CREATED, URI.create("/groups/${snapshot.id.value}"))

    fun current(snapshot: GroupSnapshot): ResponseEntity<GroupResponse> = response(snapshot, HttpStatus.OK)

    private fun response(
        snapshot: GroupSnapshot,
        status: HttpStatus,
        location: URI? = null,
    ): ResponseEntity<GroupResponse> {
        val body = GroupResponse(
            groupId = snapshot.id.value,
            displayName = snapshot.group.name.value,
            settlementCurrency = snapshot.group.settlementCurrency.code,
            settlementCurrencyLocked = snapshot.group.settlementCurrencyLocked,
            participants = snapshot.group.participants,
        )
        val headers = noStoreHeaders().apply {
            eTag = "\"${snapshot.revision.value}\""
            this.location = location
        }
        return ResponseEntity(body, headers, status)
    }
}

internal fun noStoreHeaders(): HttpHeaders = HttpHeaders().apply { cacheControl = "no-store" }

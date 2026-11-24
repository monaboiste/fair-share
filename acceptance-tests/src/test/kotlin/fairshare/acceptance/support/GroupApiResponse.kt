package fairshare.acceptance.support

import tools.jackson.databind.JsonNode
import java.net.http.HttpHeaders

internal class GroupApiResponse(val status: Int, private val headers: HttpHeaders, private val body: JsonNode) {
    val revision: String get() = headers.firstValue("ETag").orElseThrow()
    val location: String get() = headers.firstValue("Location").orElseThrow()
    val problemType: String get() = body["type"].stringValue()

    val group: GroupRepresentation
        get() = GroupRepresentation(
            body["groupId"].stringValue(),
            body["displayName"].stringValue(),
            body["settlementCurrency"].stringValue(),
            body["participants"].toList(),
        )
}

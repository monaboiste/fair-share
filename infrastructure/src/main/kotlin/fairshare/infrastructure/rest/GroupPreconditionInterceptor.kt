package fairshare.infrastructure.rest

import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupRevisionPrecondition
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.servlet.HandlerInterceptor
import java.util.Collections

internal const val GROUP_PRECONDITION = "groupPrecondition"

internal class GroupPreconditionInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.method == "PATCH") {
            val values = Collections.list(request.getHeaders(HttpHeaders.IF_MATCH))
            request.setAttribute(GROUP_PRECONDITION, groupRevisionPrecondition(values))
        }
        return true
    }
}

internal class MissingGroupPrecondition : RuntimeException()

private fun groupRevisionPrecondition(values: List<String>): GroupRevisionPrecondition {
    if (values.isEmpty()) throw MissingGroupPrecondition()
    val singleTag = values.singleOrNull()?.let(strongTag::matchEntire) ?: throw GroupRequestFailure()
    return GroupRevisionPrecondition(GroupRevision(singleTag.groupValues[1]))
}

private val strongTag = Regex("""[ \t]*"([\x21\x23-\x7E\x80-\xFF]*)"[ \t]*""")

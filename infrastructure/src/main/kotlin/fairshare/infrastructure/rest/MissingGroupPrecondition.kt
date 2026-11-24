package fairshare.infrastructure.rest

import fairshare.application.group.GroupRevision
import fairshare.application.group.GroupRevisionPrecondition
import org.springframework.http.HttpHeaders

internal class MissingGroupPrecondition : RuntimeException()

internal fun groupRevisionPrecondition(headers: HttpHeaders): GroupRevisionPrecondition {
    val values = headers[HttpHeaders.IF_MATCH] ?: throw MissingGroupPrecondition()
    val singleTag = values.singleOrNull()?.let(strongTag::matchEntire) ?: throw GroupRequestFailure()
    return GroupRevisionPrecondition(GroupRevision(singleTag.groupValues[1]))
}

private val strongTag = Regex("""[ \t]*"([\x21\x23-\x7E\x80-\xFF]*)"[ \t]*""")

package fairshare.infrastructure.http

import fairshare.application.group.GroupRevision
import org.springframework.http.HttpHeaders

internal class MissingGroupPrecondition : RuntimeException()

internal fun groupRevision(headers: HttpHeaders): GroupRevision {
    val values = headers[HttpHeaders.IF_MATCH] ?: throw MissingGroupPrecondition()
    val singleTag = values.singleOrNull()?.let(strongTag::matchEntire) ?: throw GroupRequestFailure()
    return GroupRevision(singleTag.groupValues[1])
}

private val strongTag = Regex("""[ \t]*"([\x21\x23-\x7E\x80-\xFF]*)"[ \t]*""")

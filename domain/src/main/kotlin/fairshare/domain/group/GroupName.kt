package fairshare.domain.group

import fairshare.domain.group.exception.InvalidGroupNameException

public data class GroupName(public val value: String) {
    init {
        if (value.isBlank()) throw InvalidGroupNameException()
    }
}

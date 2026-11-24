package fairshare.domain.group

import java.util.UUID

public data class GroupId(public val value: String) {
    public companion object {
        public fun random(): GroupId = GroupId("${UUID.randomUUID()}")
    }
}

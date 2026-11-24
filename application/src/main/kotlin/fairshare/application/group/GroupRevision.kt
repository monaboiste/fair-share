package fairshare.application.group

import java.util.UUID

public data class GroupRevision(public val value: String) {
    public companion object {
        public fun random(): GroupRevision = GroupRevision("${UUID.randomUUID()}")
    }
}

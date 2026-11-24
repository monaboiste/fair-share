package fairshare.application.group

import fairshare.domain.group.Group
import fairshare.domain.group.GroupId

public data class GroupSnapshot(public val id: GroupId, public val revision: GroupRevision, public val group: Group)

package fairshare.application.group

import fairshare.domain.group.GroupEvent

public data class GroupStream(public val revision: GroupRevision, public val events: List<GroupEvent>)

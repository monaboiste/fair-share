package fairshare.application.group

public data class GroupStream(public val revision: GroupRevision, public val events: List<GroupEventEnvelope>)

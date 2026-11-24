package fairshare.application.group

import fairshare.domain.group.GroupId
import fairshare.domain.group.events.GroupEvent
import java.time.Instant

public data class GroupEventEnvelope(
    public val eventId: EventId,
    public val groupId: GroupId,
    public val sequence: Long,
    public val revision: GroupRevision,
    public val recordedAt: Instant,
    public val event: GroupEvent,
)

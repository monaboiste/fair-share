package fairshare.application.group

import fairshare.domain.group.GroupEvent

@JvmInline
public value class GroupId(public val value: String)

@JvmInline
public value class GroupRevision(public val value: String)

public data class GroupStream(public val revision: GroupRevision, public val events: List<GroupEvent>)

public interface GroupStore {
    public fun load(id: GroupId): GroupStream?

    public fun append(id: GroupId, expected: GroupRevision?, event: GroupEvent): GroupAppend
}

public sealed interface GroupAppend {
    public data class Committed(public val revision: GroupRevision) : GroupAppend
    public data object RevisionMismatch : GroupAppend
}

public class GroupStoreUnavailable(cause: Throwable) : RuntimeException(cause)

public class GroupAppendOutcomeUnknown(cause: Throwable) : RuntimeException(cause)

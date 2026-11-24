package fairshare.application.group

public sealed interface GroupAppend {
    public class Committed(public val revision: GroupRevision) : GroupAppend
    public data object RevisionMismatch : GroupAppend
}

public class GroupStoreUnavailable(cause: Throwable) : RuntimeException(cause)

public class GroupAppendOutcomeUnknown(cause: Throwable) : RuntimeException(cause)

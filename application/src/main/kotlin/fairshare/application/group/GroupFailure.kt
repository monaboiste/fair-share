package fairshare.application.group

public sealed class GroupFailure : RuntimeException() {
    public class NotFound : GroupFailure()
    public class RevisionMismatch : GroupFailure()
}

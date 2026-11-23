package fairshare.application.group

public sealed class GroupFailure : RuntimeException() {
    public class NotFound : GroupFailure()
    public class RevisionMismatch : GroupFailure()
    public class InvalidGroup(public val violations: List<GroupInputViolation>) : GroupFailure()
}

public enum class GroupInputViolation {
    BLANK_DISPLAY_NAME,
    INVALID_SETTLEMENT_CURRENCY,
}

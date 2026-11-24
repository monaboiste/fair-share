package fairshare.application.group

public class GroupRevisionPrecondition(public val expected: GroupRevision) {
    internal fun verify(current: GroupRevision) {
        if (current != expected) throw GroupFailure.RevisionMismatch()
    }
}

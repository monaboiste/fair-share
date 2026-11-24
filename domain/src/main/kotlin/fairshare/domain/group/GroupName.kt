package fairshare.domain.group

@ConsistentCopyVisibility
public data class GroupName private constructor(public val value: String) {
    public companion object {
        public fun parse(value: String): GroupName? = if (value.isBlank()) null else GroupName(value)
    }
}

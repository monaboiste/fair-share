package fairshare.domain.events

@JvmInline
public value class StreamRevision(public val value: Long) {
    init {
        require(value >= 0L) { "Stream revision must not be negative" }
    }

    public companion object {
        public val INITIAL: StreamRevision = StreamRevision(0)
    }
}

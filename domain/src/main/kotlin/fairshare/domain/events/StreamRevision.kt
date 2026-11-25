package fairshare.domain.events

/** Counts committed batches within a stream, starting at one. Zero denotes a stream not yet created. */
@JvmInline
public value class StreamRevision(public val value: Long) {
    init {
        require(value >= 0L) { "Stream revision must not be negative" }
    }

    public companion object {
        /** Expected revision when creating a stream. */
        public val INITIAL: StreamRevision = StreamRevision(0)
    }
}

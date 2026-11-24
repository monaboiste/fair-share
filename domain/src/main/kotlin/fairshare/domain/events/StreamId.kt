package fairshare.domain.events

@JvmInline
public value class StreamId(public val value: String) {
    init {
        require(value.isNotBlank()) { "Stream ID must not be blank" }
        require(value.isValidUnicode()) { "Stream ID must contain valid Unicode" }
    }

    private fun String.isValidUnicode(): Boolean {
        var i = 0
        while (i < length) {
            val c = this[i]
            if (c.isSurrogate()) {
                if (!c.isHighSurrogate() || i + 1 >= length || !this[i + 1].isLowSurrogate()) {
                    return false
                }
                i++
            }
            i++
        }
        return true
    }
}

package fairshare.infrastructure.persistence

public data class SqliteGroupStoreSettings(
    public val jdbcUrl: String = "jdbc:sqlite:fair-share.db",
    public val busyTimeoutMillis: Int = 5_000,
    public val synchronous: Synchronous = Synchronous.EXTRA,
) {
    init {
        require(jdbcUrl.startsWith("jdbc:sqlite:")) { "Expected a SQLite JDBC URL" }
        require(busyTimeoutMillis >= 0) { "SQLite busy timeout must be non-negative" }
    }

    public enum class Synchronous { FULL, EXTRA }
}

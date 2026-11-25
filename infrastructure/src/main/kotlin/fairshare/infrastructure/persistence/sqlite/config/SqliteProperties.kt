package fairshare.infrastructure.persistence.sqlite.config

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import org.sqlite.SQLiteConfig

@Validated
@ConfigurationProperties("fair-share.persistence.sqlite")
internal data class SqliteProperties(
    @field:Pattern(regexp = "^jdbc:sqlite:.+")
    val jdbcUrl: String,
    @field:PositiveOrZero
    val busyTimeoutMillis: Int,
    val synchronous: SQLiteConfig.SynchronousMode,
    val journalMode: SQLiteConfig.JournalMode = SQLiteConfig.JournalMode.WAL,
) {
    init {
        require(journalMode == SQLiteConfig.JournalMode.WAL || journalMode == SQLiteConfig.JournalMode.DELETE) {
            "SQLite journal mode must be WAL or DELETE; got $journalMode"
        }
    }
}

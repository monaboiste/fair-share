package fairshare.bootstrap

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("fair-share.persistence.sqlite")
internal data class PersistenceProperties(
    @field:Pattern(
        regexp = "jdbc:sqlite:(?!file:|:memory:)(?=.*\\S)[^?]+",
        message = "must be a SQLite JDBC URL with a plain database file path",
    )
    val jdbcUrl: String,
    @field:PositiveOrZero
    val busyTimeoutMillis: Int,
    val synchronous: Synchronous,
) {
    enum class Synchronous { FULL, EXTRA }
}

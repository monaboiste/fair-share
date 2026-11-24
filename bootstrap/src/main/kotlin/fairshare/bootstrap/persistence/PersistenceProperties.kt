package fairshare.bootstrap.persistence

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import org.sqlite.SQLiteConfig

@Validated
@ConfigurationProperties("fair-share.persistence.sqlite")
internal data class PersistenceProperties(
    @field:Pattern(regexp = "^jdbc:sqlite:.+")
    val jdbcUrl: String,
    @field:PositiveOrZero
    val busyTimeoutMillis: Int,
    val synchronous: SQLiteConfig.SynchronousMode,
)

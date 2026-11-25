package fairshare.infrastructure.persistence.sqlite.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SqliteProperties::class)
internal class SqliteConfiguration {

    @Bean
    fun dataSource(properties: SqliteProperties): DataSource {
        val config = SQLiteConfig()
        config.enforceForeignKeys(true)
        config.setBusyTimeout(properties.busyTimeoutMillis)
        config.setSynchronous(properties.synchronous)

        return SQLiteDataSource(config).apply {
            url = properties.jdbcUrl
        }
    }
}

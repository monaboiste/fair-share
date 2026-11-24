package fairshare.bootstrap.persistence

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PersistenceProperties::class)
internal class PersistenceConfiguration {
    @Bean
    fun dataSource(properties: PersistenceProperties): DataSource {
        val config = SQLiteConfig()
        config.setBusyTimeout(properties.busyTimeoutMillis)
        config.setSynchronous(properties.synchronous)

        val dataSource = SQLiteDataSource(config)
        dataSource.url = properties.jdbcUrl

        return dataSource
    }
}

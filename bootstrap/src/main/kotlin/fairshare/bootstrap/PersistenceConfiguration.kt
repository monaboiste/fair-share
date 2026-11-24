package fairshare.bootstrap

import fairshare.application.group.GroupEventStore
import fairshare.application.group.GroupViewRepository
import fairshare.infrastructure.persistence.sqlite.SqliteGroupEventStore
import fairshare.infrastructure.persistence.sqlite.SqliteGroupViewRepository
import fairshare.infrastructure.persistence.sqlite.SqliteSchemaInitializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.time.Clock
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PersistenceProperties::class)
internal class PersistenceConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun dataSource(properties: PersistenceProperties): DataSource {
        val options = SQLiteConfig().apply {
            setBusyTimeout(properties.busyTimeoutMillis)
            setPragma(SQLiteConfig.Pragma.SYNCHRONOUS, properties.synchronous.name)
        }
        val dataSource = SQLiteDataSource(options).apply { url = properties.jdbcUrl }
        SqliteSchemaInitializer(dataSource).initialize()
        return dataSource
    }

    @Bean
    fun groupEventStore(dataSource: DataSource, clock: Clock): GroupEventStore =
        SqliteGroupEventStore(dataSource, clock)

    @Bean
    fun groupViewRepository(dataSource: DataSource): GroupViewRepository = SqliteGroupViewRepository(dataSource)
}

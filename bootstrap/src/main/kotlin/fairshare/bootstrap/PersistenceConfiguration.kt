package fairshare.bootstrap

import fairshare.application.group.GroupStore
import fairshare.infrastructure.persistence.SqliteGroupStore
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PersistenceProperties::class)
internal class PersistenceConfiguration {
    @Bean
    fun groupStore(properties: PersistenceProperties): GroupStore = SqliteGroupStore(properties.sqlite)
}

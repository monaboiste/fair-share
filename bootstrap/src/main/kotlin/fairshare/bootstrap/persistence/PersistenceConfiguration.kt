package fairshare.bootstrap.persistence

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan("fairshare.infrastructure.persistence.sqlite.config")
internal class PersistenceConfiguration

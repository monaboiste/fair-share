package fairshare.bootstrap.rest

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ComponentScan("fairshare.infrastructure.rest.config")
internal class RestConfiguration

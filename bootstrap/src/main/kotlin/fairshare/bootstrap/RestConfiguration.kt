package fairshare.bootstrap

import fairshare.infrastructure.rest.RestWebConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.net.URI

@Configuration(proxyBeanMethods = false)
@Import(RestWebConfiguration::class)
@EnableConfigurationProperties(RestProperties::class)
internal class RestConfiguration {
    @Bean
    fun problemOrigin(properties: RestProperties): URI = properties.problemOrigin
}

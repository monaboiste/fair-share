package fairshare.infrastructure.rest

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.type.LogicalType

@Configuration(proxyBeanMethods = false)
internal class RestJsonConfiguration {
    @Bean
    fun strictTextualValues(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        builder.withCoercionConfig(LogicalType.Textual) { config ->
            config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
        }
    }
}

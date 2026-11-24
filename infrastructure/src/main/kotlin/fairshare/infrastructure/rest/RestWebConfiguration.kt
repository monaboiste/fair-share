package fairshare.infrastructure.rest

import org.springframework.boot.jackson.autoconfigure.JsonFactoryBuilderCustomizer
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.handler.MappedInterceptor
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.type.LogicalType

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = [RestWebConfiguration::class])
public class RestWebConfiguration {
    @Bean
    internal fun rejectDuplicateFields(): JsonFactoryBuilderCustomizer = JsonFactoryBuilderCustomizer {
        it.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
    }

    @Bean
    internal fun strictRestJson(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        builder.enable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
        )
        builder.withCoercionConfig(LogicalType.Textual) { coercion ->
            coercion.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
            coercion.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            coercion.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
        }
    }

    @Bean
    internal fun groupPrecondition(): MappedInterceptor =
        MappedInterceptor(arrayOf("/groups/{groupId}/currency"), GroupPreconditionInterceptor())
}

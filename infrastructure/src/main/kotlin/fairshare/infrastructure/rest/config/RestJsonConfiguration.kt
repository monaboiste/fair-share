package fairshare.infrastructure.rest.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.type.LogicalType

@Configuration(proxyBeanMethods = false)
internal class RestJsonConfiguration {

    @Bean
    fun restJsonConverter(builder: JsonMapper.Builder): JacksonJsonHttpMessageConverter {
        builder.enable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
            DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
        )
        builder.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)

        builder.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        builder.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)

        builder.withCoercionConfig(LogicalType.Textual) { config ->
            config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
            config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
        }

        return JacksonJsonHttpMessageConverter(builder)
    }
}

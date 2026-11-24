package fairshare.bootstrap

import fairshare.application.group.ChangeGroupSettlementCurrencyUseCase
import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.application.group.GroupStore
import fairshare.infrastructure.rest.RestSettings
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature

@Configuration(proxyBeanMethods = false)
@ComponentScan("fairshare.infrastructure.rest")
@EnableConfigurationProperties(RestProperties::class)
internal class RestConfiguration {
    @Bean
    fun restSettings(properties: RestProperties): RestSettings = RestSettings(properties.problemOrigin)

    @Bean
    fun strictRestJson(): JsonMapperBuilderCustomizer = JsonMapperBuilderCustomizer { builder ->
        builder.enable(
            DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
            DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY,
        )
    }

    @Bean
    fun createGroup(store: GroupStore): CreateGroupUseCase = CreateGroupUseCase(store)

    @Bean
    fun getGroup(store: GroupStore): GetGroupUseCase = GetGroupUseCase(store)

    @Bean
    fun changeGroupSettlementCurrency(store: GroupStore): ChangeGroupSettlementCurrencyUseCase =
        ChangeGroupSettlementCurrencyUseCase(store)
}

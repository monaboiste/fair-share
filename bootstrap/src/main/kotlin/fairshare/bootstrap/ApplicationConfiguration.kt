package fairshare.bootstrap

import fairshare.application.group.ChangeGroupSettlementCurrencyUseCase
import fairshare.application.group.CreateGroupUseCase
import fairshare.application.group.GetGroupUseCase
import fairshare.application.group.GroupEventStore
import fairshare.application.group.GroupViewRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class ApplicationConfiguration {
    @Bean
    fun createGroup(store: GroupEventStore): CreateGroupUseCase = CreateGroupUseCase(store)

    @Bean
    fun getGroup(repository: GroupViewRepository): GetGroupUseCase = GetGroupUseCase(repository)

    @Bean
    fun changeGroupSettlementCurrency(store: GroupEventStore): ChangeGroupSettlementCurrencyUseCase =
        ChangeGroupSettlementCurrencyUseCase(store)
}

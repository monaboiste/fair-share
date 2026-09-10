package com.softwarearchetypes.rules.discounting.scenarios

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import com.softwarearchetypes.quantity.money.Money
import com.softwarearchetypes.rules.core.Modifier
import com.softwarearchetypes.rules.core.config.ConfigKeys
import com.softwarearchetypes.rules.core.config.RuleDefinition
import com.softwarearchetypes.rules.core.config.RuleDefinitionRepository
import com.softwarearchetypes.rules.core.config.RuleParam
import com.softwarearchetypes.rules.core.config.reflection.ReflectionBeanWriter
import com.softwarearchetypes.rules.core.config.reflection.ReflectionRuleConfigProvider
import com.softwarearchetypes.rules.core.selection.CandidateRule
import com.softwarearchetypes.rules.discounting.config.codecs.Codecs
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContextRepository
import com.softwarearchetypes.rules.discounting.fixture.client.ClientFinder
import com.softwarearchetypes.rules.discounting.fixture.client.ClientStatus
import com.softwarearchetypes.rules.discounting.fixture.config.ClientTierDynamicConfig
import com.softwarearchetypes.rules.discounting.fixture.config.ClientTierStaticConfig
import com.softwarearchetypes.rules.discounting.fixture.offer.ClientTierOfferItemModifierFactory
import com.softwarearchetypes.rules.discounting.offer.OfferItem
import com.softwarearchetypes.rules.discounting.stock.InventoryFinder
import com.softwarearchetypes.rules.discounting.stock.ProductStock
import java.time.LocalDate
import spock.lang.Specification

class ClientTierOfferItemScenarioSpec extends Specification {

    private final ClientContextRepository clientContexts = { UUID clientId ->
        new ClientContext(
                clientId,
                ClientStatus.VIP,
                Money.of(1_000_000, "PLN"),
                LocalDate.now().minusYears(4)
        )
    } as ClientContextRepository

    private final RuleDefinitionRepository ruleRepository = new InMemoryRuleDefinitionRepository()

    def "visitor selects the VIP discount"() {
        given:
        def item = itemPriced(100)

        when:
        def modified = new ClientTierOfferItemModifierFactory(null, null)
                .createDiscountModifier(ClientStatus.VIP)
                .modify(item)

        then:
        modified.finalPrice == Money.of(85, "PLN")
    }

    def "static configuration selects applicable discounts"() {
        given:
        def factory = new ClientTierOfferItemModifierFactory(clientContexts, new ClientTierStaticConfig())

        when:
        Modifier<OfferItem> modifier = factory.createDiscountModifierUsingProvider(UUID.randomUUID())

        then:
        modifier.modify(itemPriced(100)).finalPrice == Money.of(80, "PLN")
    }

    def "stored configuration reconstructs applicable discounts"() {
        given:
        def rules = new ClientTierStaticConfig().load()
        new ConfigImporter(ruleRepository).importConfig(rules)
        def provider = new ReflectionRuleConfigProvider<ClientContext, OfferItem>(ruleRepository, Codecs.QUANTITY)
        def factory = new ClientTierOfferItemModifierFactory(clientContexts, provider)

        when:
        Modifier<OfferItem> modifier = factory.createDiscountModifierUsingProvider(UUID.randomUUID())

        then:
        modifier.modify(itemPriced(100)).finalPrice == Money.of(80, "PLN")
    }

    def "dynamic configuration survives persistence"() {
        given:
        UUID productId = UUID.randomUUID()
        InventoryFinder inventory = {
            [new ProductStock(productId, Quantity.of(600, Unit.pieces()), 100)]
        } as InventoryFinder
        ClientFinder clients = [
                countVipClients: { 10L },
                countAllClients: { 100L }
        ] as ClientFinder
        def rules = new ClientTierDynamicConfig(inventory, clients).load()
        new ConfigImporter(ruleRepository).importConfig(rules)
        def provider = new ReflectionRuleConfigProvider<ClientContext, OfferItem>(ruleRepository, Codecs.QUANTITY)

        when:
        def restoredRules = provider.load()
        def modified = restoredRules.first().modifier().modify(itemPriced(productId, 100))

        then:
        restoredRules.size() == rules.size()
        modified.finalPrice == Money.of(75, "PLN")
    }

    private static OfferItem itemPriced(Number amount) {
        itemPriced(UUID.randomUUID(), amount)
    }

    private static OfferItem itemPriced(UUID productId, Number amount) {
        new OfferItem(productId, Quantity.of(1, Unit.kilograms()), Money.of(amount, "PLN"))
    }

    private static class ConfigImporter {

        private final RuleDefinitionRepository ruleRepository
        private final ReflectionBeanWriter beanWriter = new ReflectionBeanWriter(Codecs.QUANTITY)

        ConfigImporter(RuleDefinitionRepository ruleRepository) {
            this.ruleRepository = ruleRepository
        }

        void importConfig(List<CandidateRule<ClientContext, OfferItem>> rules) {
            rules.each { rule ->
                Map<String, String> params = [:]
                beanWriter.writeBean(ConfigKeys.MODIFIER_PREFIX, rule.modifier(), params)
                UUID ruleId = ruleRepository.insert(new RuleDefinition(null, humanReadableName(rule.modifier())))
                params.each { name, value -> ruleRepository.insertParam(new RuleParam(ruleId, name, value)) }

                params.clear()
                beanWriter.writeBean(ConfigKeys.SELECTION_PREDICATE_PREFIX, rule.appliesTo(), params)
                params.each { name, value -> ruleRepository.insertParam(new RuleParam(ruleId, name, value)) }
            }
        }

        private static String humanReadableName(Modifier<OfferItem> modifier) {
            try {
                def result = modifier.class.getMethod("getName").invoke(modifier)
                return result == null ? modifier.class.simpleName : result.toString()
            } catch (ReflectiveOperationException ignored) {
                return modifier.class.simpleName
            }
        }
    }

    private static class InMemoryRuleDefinitionRepository implements RuleDefinitionRepository {

        private final List<RuleDefinition> definitions = []
        private final List<RuleParam> params = []

        @Override
        List<RuleDefinition> findAllDefinitions() {
            definitions
        }

        @Override
        List<RuleParam> findParamsByRuleId(UUID id) {
            params.findAll { param -> param.ruleId() == id }
        }

        @Override
        UUID insert(RuleDefinition definition) {
            def stored = new RuleDefinition(UUID.randomUUID(), definition.name())
            definitions.add(stored)
            stored.id()
        }

        @Override
        void insertParam(RuleParam param) {
            params.add(param)
        }
    }
}

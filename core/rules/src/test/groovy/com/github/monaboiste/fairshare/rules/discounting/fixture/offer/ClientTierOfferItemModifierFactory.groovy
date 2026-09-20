package com.github.monaboiste.fairshare.rules.discounting.fixture.offer

import com.github.monaboiste.fairshare.quantity.money.Percentage
import com.github.monaboiste.fairshare.rules.core.Modifier
import com.github.monaboiste.fairshare.rules.core.selection.CandidateRule
import com.github.monaboiste.fairshare.rules.core.selection.RuleConfigProvider
import com.github.monaboiste.fairshare.rules.core.selection.RuleSelector
import com.github.monaboiste.fairshare.rules.discounting.OfferItemModifier
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientContext
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientContextRepository
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientStatus
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientStatusVisitor
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.simple.PercentageOfferItemModifier

class ClientTierOfferItemModifierFactory {

    private final ClientContextRepository clientContextRepository
    private final RuleConfigProvider<ClientContext, OfferItem> configProvider

    ClientTierOfferItemModifierFactory(
            ClientContextRepository clientContextRepository,
            RuleConfigProvider<ClientContext, OfferItem> configProvider) {
        this.clientContextRepository = clientContextRepository
        this.configProvider = configProvider
    }

    OfferItemModifier createDiscountModifier(ClientStatus status) {
        status.accept(new ClientTierOfferItemModifierVisitor())
    }

    Modifier<OfferItem> createDiscountModifierUsingProvider(UUID clientId) {
        RuleConfigProvider<ClientContext, OfferItem> provider =
                Objects.requireNonNull(configProvider, 'Config provider is required')
        ClientContextRepository repository =
                Objects.requireNonNull(clientContextRepository, 'Client context repository is required')

        List<CandidateRule<ClientContext, OfferItem>> rules = provider.load()
        ClientContext clientContext = repository.loadClientContext(clientId)

        RuleSelector.select(clientContext, rules)
    }
}

class ClientTierOfferItemModifierVisitor implements ClientStatusVisitor<OfferItemModifier> {

    @Override
    OfferItemModifier visitStandard() {
        new PercentageOfferItemModifier('My friend', Percentage.ofFraction(0.05))
    }

    @Override
    OfferItemModifier visitVIP() {
        new PercentageOfferItemModifier('VIP', Percentage.ofFraction(0.15))
    }

    @Override
    OfferItemModifier visitGold() {
        new PercentageOfferItemModifier('Gold', Percentage.of(25))
    }
}

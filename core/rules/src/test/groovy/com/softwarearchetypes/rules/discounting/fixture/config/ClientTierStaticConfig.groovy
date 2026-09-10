package com.softwarearchetypes.rules.discounting.fixture.config

import com.softwarearchetypes.quantity.money.Money
import com.softwarearchetypes.quantity.money.Percentage
import com.softwarearchetypes.rules.core.ConfigurableModifier
import com.softwarearchetypes.rules.core.Modifier
import com.softwarearchetypes.rules.core.selection.CandidateRule
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext
import com.softwarearchetypes.rules.discounting.fixture.client.ClientStatus
import com.softwarearchetypes.rules.discounting.fixture.client.rules.ExpensesRule
import com.softwarearchetypes.rules.discounting.fixture.client.rules.StatusRule
import com.softwarearchetypes.rules.discounting.fixture.client.rules.TimeBeingCustomer
import com.softwarearchetypes.rules.discounting.offer.OfferItem
import com.softwarearchetypes.rules.discounting.offer.PriceChangeApplicator
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier.PercentageFromBase
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians.EmptyGuardian
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate

import java.util.function.Predicate

class ClientTierStaticConfig implements ConfigProvider {

    @Override
    List<CandidateRule<ClientContext, OfferItem>> load() {
        Modifier<OfferItem> longTermVipModifier = new ConfigurableModifier<OfferItem, Money>(
                "3 years of VIPs",
                new MoreExpensiveThanPredicate(Money.of(50, "PLN")),
                new PercentageFromBase(Percentage.of(10)),
                EmptyGuardian.INSTANCE,
                PriceChangeApplicator.INSTANCE
        )
        Predicate<ClientContext> longTermVip = StatusRule.of(ClientStatus.VIP)
                .and(TimeBeingCustomer.ofYears(3))

        Modifier<OfferItem> highSpendingVipModifier = new ConfigurableModifier<OfferItem, Money>(
                "VIPs - big fish",
                new MoreExpensiveThanPredicate(Money.of(100, "PLN")),
                new PercentageFromBase(Percentage.of(10)),
                EmptyGuardian.INSTANCE,
                PriceChangeApplicator.INSTANCE
        )
        Predicate<ClientContext> highSpendingVip = StatusRule.of(ClientStatus.VIP)
                .and(ExpensesRule.of(Money.of(500000, "PLN")))

        [
                new CandidateRule<>(longTermVipModifier, longTermVip),
                new CandidateRule<>(highSpendingVipModifier, highSpendingVip)
        ]
    }
}

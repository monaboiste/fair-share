package com.github.monaboiste.fairshare.rules.discounting.fixture.config

import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.quantity.money.Percentage
import com.github.monaboiste.fairshare.rules.core.ConfigurableModifier
import com.github.monaboiste.fairshare.rules.core.Modifier
import com.github.monaboiste.fairshare.rules.core.selection.CandidateRule
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientContext
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.ClientStatus
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.rules.ExpensesRule
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.rules.StatusRule
import com.github.monaboiste.fairshare.rules.discounting.fixture.client.rules.TimeBeingCustomer
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem
import com.github.monaboiste.fairshare.rules.discounting.offer.PriceChangeApplicator
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.applier.PercentageFromBase
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.guardians.EmptyGuardian
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate

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

package com.github.monaboiste.fairshare.rules.discounting

import com.github.monaboiste.fairshare.quantity.Quantity
import com.github.monaboiste.fairshare.quantity.Unit
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.quantity.money.Percentage
import com.github.monaboiste.fairshare.rules.core.ChainModifier
import com.github.monaboiste.fairshare.rules.core.ConfigurableModifier
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem
import com.github.monaboiste.fairshare.rules.discounting.offer.PriceChangeApplicator
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.applier.Amount
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.guardians.MarginGuardian
import com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate
import spock.lang.Specification

class ChainOfferModifierSpec extends Specification {

    def "applies an allowed price modification"() {
        given:
        def modifier = new ChainModifier<OfferItem>()
                .add(new ConfigurableModifier<OfferItem, Money>(
                        "expensive line",
                        new MoreExpensiveThanPredicate(Money.of(1000, "PLN")),
                        new Amount(Money.of(100, "PLN")),
                        new MarginGuardian(Percentage.of(30)),
                        PriceChangeApplicator.INSTANCE
                ))

        expect:
        modifier.modify(itemPriced(2000)).finalPrice == Money.of(1900, "PLN")
    }

    def "guardian rejects a price below the margin"() {
        given:
        def modifier = new ChainModifier<OfferItem>()
                .add(new ConfigurableModifier<OfferItem, Money>(
                        "expensive line",
                        new MoreExpensiveThanPredicate(Money.of(1000, "PLN")),
                        new Amount(Money.of(900, "PLN")),
                        new MarginGuardian(Percentage.of(60)),
                        PriceChangeApplicator.INSTANCE
                ))

        expect:
        modifier.modify(itemPriced(2000)).finalPrice == Money.of(2000, "PLN")
    }

    private static OfferItem itemPriced(Number amount) {
        new OfferItem(UUID.randomUUID(), Quantity.of(1, Unit.kilograms()), Money.of(amount, "PLN"))
    }
}

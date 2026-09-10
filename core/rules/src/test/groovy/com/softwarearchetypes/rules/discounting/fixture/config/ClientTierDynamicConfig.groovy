package com.softwarearchetypes.rules.discounting.fixture.config

import com.softwarearchetypes.quantity.money.Money
import com.softwarearchetypes.quantity.money.Percentage
import com.softwarearchetypes.rules.core.ConfigurableModifier
import com.softwarearchetypes.rules.core.Modifier
import com.softwarearchetypes.rules.core.predicates.AlwaysPredicate
import com.softwarearchetypes.rules.core.selection.CandidateRule
import com.softwarearchetypes.rules.discounting.fixture.client.ClientContext
import com.softwarearchetypes.rules.discounting.fixture.client.ClientFinder
import com.softwarearchetypes.rules.discounting.fixture.client.ClientStatus
import com.softwarearchetypes.rules.discounting.fixture.client.rules.ExpensesRule
import com.softwarearchetypes.rules.discounting.fixture.client.rules.StatusRule
import com.softwarearchetypes.rules.discounting.fixture.client.rules.TimeBeingCustomer
import com.softwarearchetypes.rules.discounting.offer.OfferItem
import com.softwarearchetypes.rules.discounting.offer.PriceChangeApplicator
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier.PercentageFromBase
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians.EmptyGuardian
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.ItemIdPredicate
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.MoreExpensiveThanPredicate
import com.softwarearchetypes.rules.discounting.stock.InventoryFinder
import com.softwarearchetypes.rules.discounting.stock.ProductStock

import java.util.function.Predicate

class ClientTierDynamicConfig implements ConfigProvider {

    private final InventoryFinder inventoryFinder
    private final ClientFinder clientFinder

    ClientTierDynamicConfig(InventoryFinder inventoryFinder, ClientFinder clientFinder) {
        this.inventoryFinder = inventoryFinder
        this.clientFinder = clientFinder
    }

    @Override
    List<CandidateRule<ClientContext, OfferItem>> load() {
        List<CandidateRule<ClientContext, OfferItem>> configuration = []

        for (ProductStock stock : inventoryFinder.findOverstockedProducts()) {
            Percentage discount = calculateDiscountFor(stock)
            Modifier<OfferItem> overstockModifier = new ConfigurableModifier<OfferItem, Money>(
                    "Overstock promo for " + stock.productId(),
                    new ItemIdPredicate(stock.productId()),
                    new PercentageFromBase(discount),
                    EmptyGuardian.INSTANCE,
                    PriceChangeApplicator.INSTANCE
            )
            configuration.add(new CandidateRule<>(overstockModifier, new AlwaysPredicate<>()))
        }

        long vipCount = clientFinder.countVipClients()
        long allCount = clientFinder.countAllClients()
        double vipRatio = allCount == 0 ? 0.0 : (double) vipCount / allCount

        if (vipRatio < 0.05) {
            Modifier<OfferItem> growVipBaseModifier = new ConfigurableModifier<OfferItem, Money>(
                    "Grow VIP base - strong promo",
                    new MoreExpensiveThanPredicate(Money.of(50, "PLN")),
                    new PercentageFromBase(Percentage.of(20)),
                    EmptyGuardian.INSTANCE,
                    PriceChangeApplicator.INSTANCE
            )
            Predicate<ClientContext> targetRegularsWithPotential = StatusRule.of(ClientStatus.STANDARD)
                    .and(ExpensesRule.of(Money.of(1000, "PLN")))
            configuration.add(new CandidateRule<>(growVipBaseModifier, targetRegularsWithPotential))
        } else {
            Modifier<OfferItem> vipRetentionModifier = new ConfigurableModifier<OfferItem, Money>(
                    "VIP retention promo",
                    new MoreExpensiveThanPredicate(Money.of(100, "PLN")),
                    new PercentageFromBase(Percentage.of(10)),
                    EmptyGuardian.INSTANCE,
                    PriceChangeApplicator.INSTANCE
            )
            Predicate<ClientContext> oldVipClients = StatusRule.of(ClientStatus.VIP)
                    .and(TimeBeingCustomer.ofYears(3))
            configuration.add(new CandidateRule<>(vipRetentionModifier, oldVipClients))
        }

        configuration
    }

    private static Percentage calculateDiscountFor(ProductStock stock) {
        int base = 5
        int extraFromQuantity = stock.quantity().amount().doubleValue() > 500
                ? 10
                : stock.quantity().amount().doubleValue() > 200 ? 5 : 0
        int extraFromDays = stock.daysInStock() > 90 ? 10 : stock.daysInStock() > 30 ? 5 : 0

        Percentage.of(Math.min(30, base + extraFromQuantity + extraFromDays))
    }
}

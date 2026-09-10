package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.core.predicates.RichLogicalPredicate;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;

public record MoreExpensiveThanPredicate(Money amount) implements RichLogicalPredicate<OfferItem> {

    @Override
    public boolean test(OfferItem offerItem) {
        return offerItem.getBasePrice().isGreaterThanOrEqualTo(amount);
    }
}

package com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.predicates;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.rules.core.predicates.RichLogicalPredicate;
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem;

public record MoreExpensiveThanPredicate(Money amount) implements RichLogicalPredicate<OfferItem> {

    @Override
    public boolean test(OfferItem offerItem) {
        return offerItem.getBasePrice().isGreaterThanOrEqualTo(amount);
    }
}

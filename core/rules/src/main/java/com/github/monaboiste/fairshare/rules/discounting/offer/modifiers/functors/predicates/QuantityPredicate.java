package com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.predicates;

import com.github.monaboiste.fairshare.quantity.Quantity;
import com.github.monaboiste.fairshare.rules.core.predicates.RichLogicalPredicate;
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem;

public record QuantityPredicate(Quantity quantity) implements RichLogicalPredicate<OfferItem> {

    @Override
    public boolean test(OfferItem offerItem) {
        return offerItem.getQuantity().compareTo(quantity) > 0;
    }
}

package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.rules.core.predicates.RichLogicalPredicate;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;

public record QuantityPredicate(Quantity quantity) implements RichLogicalPredicate<OfferItem> {

    @Override
    public boolean test(OfferItem offerItem) {
        return offerItem.getQuantity().compareTo(quantity) > 0;
    }
}

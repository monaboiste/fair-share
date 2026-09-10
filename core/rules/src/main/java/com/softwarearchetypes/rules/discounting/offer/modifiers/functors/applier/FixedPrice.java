package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public record FixedPrice(Money amount) implements Function<OfferItem, Money> {

    @Override
    public Money apply(OfferItem offerItem) {
        return amount;
    }
}

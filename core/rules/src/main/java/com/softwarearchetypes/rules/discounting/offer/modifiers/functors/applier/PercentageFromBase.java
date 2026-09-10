package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.applier;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public record PercentageFromBase(Percentage percentage) implements Function<OfferItem, Money> {

    @Override
    public Money apply(OfferItem offerItem) {
        Money discountAmount = offerItem.getBasePrice().multiply(percentage);
        return offerItem.getFinalPrice().subtract(discountAmount);
    }
}

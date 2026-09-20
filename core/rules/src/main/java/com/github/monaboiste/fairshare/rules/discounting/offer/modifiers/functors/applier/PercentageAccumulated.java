package com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.applier;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.quantity.money.Percentage;
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public record PercentageAccumulated(Percentage percentage) implements Function<OfferItem, Money> {

    @Override
    public Money apply(OfferItem offerItem) {
        Money discountAmount = offerItem.getFinalPrice().multiply(percentage);
        return offerItem.getFinalPrice().subtract(discountAmount);
    }
}

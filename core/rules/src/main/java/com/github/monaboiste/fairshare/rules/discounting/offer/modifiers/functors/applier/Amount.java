package com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.functors.applier;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem;
import java.util.function.Function;

public record Amount(Money amount) implements Function<OfferItem, Money> {

    @Override
    public Money apply(OfferItem offerItem) {
        return offerItem.getFinalPrice().subtract(amount);
    }
}

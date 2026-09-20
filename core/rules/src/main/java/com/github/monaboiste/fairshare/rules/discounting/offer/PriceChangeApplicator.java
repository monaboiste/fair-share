package com.github.monaboiste.fairshare.rules.discounting.offer;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.rules.core.ChangeApplicator;
import com.github.monaboiste.fairshare.rules.core.Modification;

public class PriceChangeApplicator implements ChangeApplicator<OfferItem, Money> {

    public static final PriceChangeApplicator INSTANCE = new PriceChangeApplicator();

    @Override
    public Money currentValue(OfferItem item) {
        return item.getFinalPrice();
    }

    @Override
    public OfferItem applyChange(OfferItem item, Money newPrice, String description) {
        return item.apply(new Modification<>(newPrice, description));
    }
}

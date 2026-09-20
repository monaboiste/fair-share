package com.github.monaboiste.fairshare.rules.discounting.offer.modifiers.simple;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.quantity.money.Percentage;
import com.github.monaboiste.fairshare.rules.core.Modification;
import com.github.monaboiste.fairshare.rules.core.NamedModifier;
import com.github.monaboiste.fairshare.rules.discounting.OfferItemModifier;
import com.github.monaboiste.fairshare.rules.discounting.offer.OfferItem;

public class PercentageOfferItemModifier extends NamedModifier<OfferItem> implements OfferItemModifier {
    private final Percentage percentage;

    public PercentageOfferItemModifier(String name, Percentage percentage) {
        super(name);
        this.percentage = percentage;
    }

    @Override
    public OfferItem modify(OfferItem item) {
        Money modification = item.getBasePrice().multiply(percentage);
        Money newPrice = item.getBasePrice().subtract(modification);
        String description = getName() + " (" + percentage + "%)";

        return item.apply(new Modification<>(newPrice, description));
    }
}

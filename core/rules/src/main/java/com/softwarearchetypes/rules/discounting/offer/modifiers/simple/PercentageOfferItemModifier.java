package com.softwarearchetypes.rules.discounting.offer.modifiers.simple;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.core.Modification;
import com.softwarearchetypes.rules.core.NamedModifier;
import com.softwarearchetypes.rules.discounting.OfferItemModifier;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;

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

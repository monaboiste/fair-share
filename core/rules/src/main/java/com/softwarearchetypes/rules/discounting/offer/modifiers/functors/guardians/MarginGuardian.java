package com.softwarearchetypes.rules.discounting.offer.modifiers.functors.guardians;

import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.util.function.Predicate;

public record MarginGuardian(Percentage minMargin) implements Predicate<OfferItem> {

    @Override
    public boolean test(OfferItem offerItem) {
        Money threshold = offerItem.getBasePrice().multiply(minMargin);
        return offerItem.getFinalPrice().isGreaterThanOrEqualTo(threshold);
    }
}

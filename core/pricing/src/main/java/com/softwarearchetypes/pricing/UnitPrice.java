package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;

public record UnitPrice(Money money) implements PricingResult {
    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }
}

package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;

public record UnitPrice(Money money) implements PricingResult {
    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }
}

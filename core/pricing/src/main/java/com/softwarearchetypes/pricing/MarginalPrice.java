package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;

public record MarginalPrice(Money money) implements PricingResult {
    @Override
    public Interpretation interpretation() {
        return Interpretation.MARGINAL;
    }
}

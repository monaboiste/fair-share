package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;

public record UnitPrice(Money money) implements PricingResult {
    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }
}

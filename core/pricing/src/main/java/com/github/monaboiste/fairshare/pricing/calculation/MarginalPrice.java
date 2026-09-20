package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;

public record MarginalPrice(Money money) implements PricingResult {
    @Override
    public Interpretation interpretation() {
        return Interpretation.MARGINAL;
    }
}

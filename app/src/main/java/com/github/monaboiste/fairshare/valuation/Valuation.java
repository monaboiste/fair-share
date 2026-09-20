package com.github.monaboiste.fairshare.valuation;

import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.pricing.component.SimpleComponentVersion;
import com.github.monaboiste.fairshare.quantity.money.Money;

public record Valuation(PricingResult result, ExchangeRate exchangeRate, SimpleComponentVersion componentVersion) {
    public Money money() {
        return result.money();
    }
}

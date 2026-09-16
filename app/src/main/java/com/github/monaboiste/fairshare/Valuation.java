package com.github.monaboiste.fairshare;

import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.pricing.component.SimpleComponentVersion;
import com.softwarearchetypes.quantity.money.Money;

public record Valuation(PricingResult result, ExchangeRate exchangeRate, SimpleComponentVersion componentVersion) {
    public Money money() {
        return result.money();
    }
}

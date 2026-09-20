package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;

final class PricingResults {

    static PricingResult of(Interpretation interpretation, Money money) {
        return switch (interpretation) {
            case TOTAL -> new TotalPrice(money);
            case UNIT -> new UnitPrice(money);
            case MARGINAL -> new MarginalPrice(money);
        };
    }

    private PricingResults() {}
}

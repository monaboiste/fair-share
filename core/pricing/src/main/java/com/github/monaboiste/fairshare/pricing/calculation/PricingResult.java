package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;

public interface PricingResult {
    Money money();

    Interpretation interpretation();

    default String describe() {
        return "%s: %s".formatted(interpretation().describe(), money());
    }
}

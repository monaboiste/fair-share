package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;

public interface PricingResult {
    Money money();

    Interpretation interpretation();

    default String describe() {
        return "%s: %s".formatted(interpretation().describe(), money());
    }
}

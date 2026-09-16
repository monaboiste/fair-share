package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;

public interface PricingResult {
    Money money();

    Interpretation interpretation();
}

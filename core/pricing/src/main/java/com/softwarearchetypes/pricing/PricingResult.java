package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;

public interface PricingResult {
    Money money();

    Interpretation interpretation();
}

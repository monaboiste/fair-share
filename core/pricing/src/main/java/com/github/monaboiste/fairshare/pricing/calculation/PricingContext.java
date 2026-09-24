package com.github.monaboiste.fairshare.pricing.calculation;

import java.time.LocalDateTime;
import javax.money.CurrencyUnit;

/**
 * Parameters that describe the evaluation as a whole rather than an input of a single calculator.
 *
 * <p>{@link #CURRENCY} is the expected currency of every result in the evaluation and is required by every component.
 * {@link #TIMESTAMP} is the point in time that selects component versions.
 */
public final class PricingContext {

    public static final ParameterKey<CurrencyUnit> CURRENCY = new ParameterKey<>("currency", CurrencyUnit.class);
    public static final ParameterKey<LocalDateTime> TIMESTAMP = new ParameterKey<>("timestamp", LocalDateTime.class);

    private PricingContext() {}
}

package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculates a configured percentage of the {@code baseAmount} parameter. */
record PercentageCalculator(CalculatorId id, String name, BigDecimal percentageRate) implements Calculator {

    private static final ParameterKey<Money> BASE_AMOUNT = new ParameterKey<>("baseAmount", Money.class);

    public PercentageCalculator(String name, BigDecimal percentageRate) {
        this(CalculatorId.generate(), name, percentageRate);
    }

    @Override
    public PricingResult calculate(Parameters params) {
        Money baseAmount = params.get(BASE_AMOUNT);
        BigDecimal rate = percentageRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        Money result = baseAmount.multiply(rate);
        return new TotalPrice(Money.of(result.value().setScale(2, RoundingMode.HALF_UP), result.currency()));
    }

    @Override
    public String formula() {
        return "baseAmount × " + percentageRate + "%";
    }

    @Override
    public String describe() {
        return "Percentage: " + percentageRate + "% of base amount";
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

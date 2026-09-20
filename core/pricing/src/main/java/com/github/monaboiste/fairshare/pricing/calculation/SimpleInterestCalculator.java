package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

record SimpleInterestCalculator(CalculatorId id, String name, BigDecimal annualRate) implements Calculator {

    private static final ParameterKey<Money> BASE = new ParameterKey<>("base", Money.class);
    private static final ParameterKey<ChronoUnit> UNIT = new ParameterKey<>("unit", ChronoUnit.class);
    private static final int SCALE = 10;

    public SimpleInterestCalculator(String name, BigDecimal annualRate) {
        this(CalculatorId.generate(), name, annualRate);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {

        Money base = parameters.get(BASE);
        ChronoUnit unit = parameters.get(UNIT);

        BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        BigDecimal unitRate = rate.divide(unitsPerYear(unit), SCALE, RoundingMode.HALF_UP);

        return new TotalPrice(base.multiply(unitRate));
    }

    @Override
    public String describe() {
        return "Annual interest calculator - calculates " + annualRate
                + "% annual interest based on base and time unit";
    }

    @Override
    public String formula() {
        return ("f(base, unit) = base × (rate/100) × (1/unitsPerYear(unit))%n" + "where rate = %s%%")
                .formatted(annualRate);
    }

    @Override
    public CalculatorId getId() {
        return id;
    }

    private BigDecimal unitsPerYear(ChronoUnit unit) {
        return switch (unit) {
            case DAYS -> BigDecimal.valueOf(365);
            case WEEKS -> BigDecimal.valueOf(52);
            case MONTHS -> BigDecimal.valueOf(12);
            case YEARS -> BigDecimal.valueOf(1);
            default -> throw new IllegalArgumentException("Unsupported unit for annual calculation: " + unit);
        };
    }
}

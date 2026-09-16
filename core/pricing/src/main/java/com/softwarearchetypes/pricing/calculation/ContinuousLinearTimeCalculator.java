package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Calculates a price by linearly interpolating between two {@link Instant} endpoints.
 *
 * <p>The query time must be within the inclusive endpoint range.
 */
record ContinuousLinearTimeCalculator(
        CalculatorId id,
        String name,
        Instant startTime,
        Money startPrice,
        Instant endTime,
        Money endPrice,
        Interpretation interpretation)
        implements Calculator {

    private static final ParameterKey<Instant> TIME = new ParameterKey<>("time", Instant.class);

    public ContinuousLinearTimeCalculator(
            String name, Instant startTime, Money startPrice, Instant endTime, Money endPrice) {
        this(CalculatorId.generate(), name, startTime, startPrice, endTime, endPrice, Interpretation.TOTAL);
    }

    public ContinuousLinearTimeCalculator(
            String name,
            Instant startTime,
            Money startPrice,
            Instant endTime,
            Money endPrice,
            Interpretation interpretation) {
        this(CalculatorId.generate(), name, startTime, startPrice, endTime, endPrice, interpretation);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {

        Instant queryTime = parameters.get(TIME);

        if (queryTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Query time %s is before start time %s".formatted(queryTime, startTime));
        }
        if (queryTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Query time %s is after end time %s".formatted(queryTime, endTime));
        }
        long totalSeconds = Duration.between(startTime, endTime).toSeconds();
        long elapsedSeconds = Duration.between(startTime, queryTime).toSeconds();

        BigDecimal progress =
                BigDecimal.valueOf(elapsedSeconds).divide(BigDecimal.valueOf(totalSeconds), 10, RoundingMode.HALF_UP);

        Money priceRange = endPrice.subtract(startPrice);
        Money interpolatedIncrease = priceRange.multiply(progress);

        return PricingResults.of(interpretation, startPrice.add(interpolatedIncrease));
    }

    @Override
    public String describe() {
        return String.format(
                "Continuous linear time calculator - from %s to %s between %s and %s",
                startPrice, endPrice, startTime, endTime);
    }

    @Override
    public String formula() {
        return ("f(t) = startPrice + progress × (endPrice - startPrice)%n"
                        + "where progress = (t - startTime) / (endTime - startTime)%n"
                        + "domain: t ∈ [%s, %s]")
                .formatted(startTime, endTime);
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

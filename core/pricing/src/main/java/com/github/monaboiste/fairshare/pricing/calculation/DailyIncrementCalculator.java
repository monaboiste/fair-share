package com.github.monaboiste.fairshare.pricing.calculation;

import static java.time.temporal.ChronoUnit.DAYS;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Calculates a price that increases by a fixed amount for each full day after the start date.
 *
 * <p>The price is constant throughout each day.
 */
record DailyIncrementCalculator(
        CalculatorId id,
        String name,
        LocalDate startDate,
        Money startPrice,
        Money dailyIncrement,
        Interpretation interpretation)
        implements Calculator {

    private static final ParameterKey<LocalDate> DATE = new ParameterKey<>("date", LocalDate.class);

    public DailyIncrementCalculator(String name, LocalDate startDate, Money startPrice, Money dailyIncrement) {
        this(CalculatorId.generate(), name, startDate, startPrice, dailyIncrement, Interpretation.TOTAL);
    }

    public DailyIncrementCalculator(
            String name, LocalDate startDate, Money startPrice, Money dailyIncrement, Interpretation interpretation) {
        this(CalculatorId.generate(), name, startDate, startPrice, dailyIncrement, interpretation);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {
        LocalDate date = parameters.get(DATE);

        long daysFromStart = DAYS.between(startDate, date);

        BigDecimal daysDecimal = BigDecimal.valueOf(daysFromStart);
        Money totalIncrement = dailyIncrement.multiply(daysDecimal);

        return PricingResults.of(interpretation, startPrice.add(totalIncrement));
    }

    @Override
    public String describe() {
        return String.format(
                "Daily increment calculator - starts at %s on %s, grows by %s per day",
                startPrice, startDate, dailyIncrement);
    }

    @Override
    public String formula() {
        return ("f(date) = startPrice + daysFromStart × dailyIncrement%n"
                        + "where:%n  startDate = %s%n  startPrice = %s%n  dailyIncrement = %s")
                .formatted(startDate, startPrice, dailyIncrement);
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

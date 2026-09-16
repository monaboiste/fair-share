package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class Calculators {

    public static Calculator fixed(String name, Money amount) {
        return new SimpleFixedCalculator(name, amount);
    }

    public static Calculator fixed(String name, Money amount, Interpretation interpretation) {
        return new SimpleFixedCalculator(name, amount, interpretation);
    }

    public static Calculator simpleInterest(String name, BigDecimal annualRate) {
        return new SimpleInterestCalculator(name, annualRate);
    }

    public static Calculator stepFunction(String name, Money basePrice, BigDecimal stepSize, BigDecimal stepIncrement) {
        return new StepFunctionCalculator(name, basePrice, stepSize, stepIncrement);
    }

    public static Calculator stepFunction(
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            Interpretation interpretation) {
        return new StepFunctionCalculator(name, basePrice, stepSize, stepIncrement, interpretation);
    }

    public static Calculator stepFunction(
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            Interpretation interpretation,
            StepBoundary stepBoundary) {
        return new StepFunctionCalculator(name, basePrice, stepSize, stepIncrement, interpretation, stepBoundary);
    }

    public static Calculator discretePoints(String name, Map<BigDecimal, Money> points) {
        return new DiscretePointsCalculator(name, points);
    }

    public static Calculator discretePoints(String name, Map<BigDecimal, Money> points, Interpretation interpretation) {
        return new DiscretePointsCalculator(name, points, interpretation);
    }

    public static Calculator dailyIncrement(String name, LocalDate startDate, Money startPrice, Money dailyIncrement) {
        return new DailyIncrementCalculator(name, startDate, startPrice, dailyIncrement);
    }

    public static Calculator dailyIncrement(
            String name, LocalDate startDate, Money startPrice, Money dailyIncrement, Interpretation interpretation) {
        return new DailyIncrementCalculator(name, startDate, startPrice, dailyIncrement, interpretation);
    }

    public static Calculator continuousLinearTime(
            String name, Instant startTime, Money startPrice, Instant endTime, Money endPrice) {
        return new ContinuousLinearTimeCalculator(name, startTime, startPrice, endTime, endPrice);
    }

    public static Calculator continuousLinearTime(
            String name,
            Instant startTime,
            Money startPrice,
            Instant endTime,
            Money endPrice,
            Interpretation interpretation) {
        return new ContinuousLinearTimeCalculator(name, startTime, startPrice, endTime, endPrice, interpretation);
    }

    public static Calculator composite(
            String name, String rangeSelector, List<CalculatorRange> ranges, Collection<Calculator> calculators) {
        return new CompositeFunctionCalculator(name, new Ranges(rangeSelector, ranges), calculators);
    }

    public static Calculator percentage(String name, BigDecimal percentageRate) {
        return new PercentageCalculator(name, percentageRate);
    }

    private Calculators() {}
}

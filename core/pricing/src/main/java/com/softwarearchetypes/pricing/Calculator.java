package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.CalculatorType.COMPOSITE;
import static java.time.temporal.ChronoUnit.DAYS;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public interface Calculator {

    Money calculate(Parameters parameters);

    String describe();

    String formula();

    Interpretation interpretation();

    /**
     * Simulates calculation for multiple points in parameter space.
     *
     * @param points list of parameter sets to evaluate
     * @return map from each parameter set to its calculated price
     */
    default Map<Parameters, Money> simulate(List<Parameters> points) {
        Map<Parameters, Money> results = new LinkedHashMap<>();
        for (Parameters params : points) {
            results.put(params, calculate(params));
        }
        return results;
    }

    CalculatorType getType();

    CalculatorId getId();

    String name();
}

record SimpleFixedCalculator(CalculatorId id, String name, Money amount, Interpretation interpretation)
        implements Calculator {

    public SimpleFixedCalculator(String name, Money amount) {
        this(CalculatorId.generate(), name, amount, Interpretation.TOTAL);
    }

    public SimpleFixedCalculator(String name, Money amount, Interpretation interpretation) {
        this(CalculatorId.generate(), name, amount, interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        return amount;
    }

    @Override
    public String describe() {
        return getType().formatDescription(amount);
    }

    @Override
    public String formula() {
        return "f(x) = %s".formatted(amount);
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.SIMPLE_FIXED;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

record SimpleInterestCalculator(CalculatorId id, String name, BigDecimal annualRate) implements Calculator {

    private static final int SCALE = 10;

    public SimpleInterestCalculator(String name, BigDecimal annualRate) {
        this(CalculatorId.generate(), name, annualRate);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException("SimpleInterestCalculator requiredPreviousKeys %s parameters"
                    .formatted(getType().requiredCalculationFields()));
        }

        Money base = parameters.getMoney("base");
        ChronoUnit unit = (ChronoUnit) parameters.require("unit");

        BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        BigDecimal unitRate = rate.divide(unitsPerYear(unit), SCALE, RoundingMode.HALF_UP);

        return base.multiply(unitRate);
    }

    @Override
    public String describe() {
        return getType().formatDescription(annualRate);
    }

    @Override
    public String formula() {
        return ("f(base, unit) = base × (rate/100) × (1/unitsPerYear(unit))%n" + "where rate = %s%%")
                .formatted(annualRate);
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.SIMPLE_INTEREST;
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

/** Calculator that increases a base price by a fixed amount every N units. */
record StepFunctionCalculator(
        CalculatorId id,
        String name,
        Money basePrice,
        BigDecimal stepSize,
        BigDecimal stepIncrement,
        Interpretation interpretation,
        StepBoundary stepBoundary)
        implements Calculator {

    public StepFunctionCalculator(
            CalculatorId id,
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            @Nullable Interpretation interpretation,
            @Nullable StepBoundary stepBoundary) {
        this.id = id;
        this.name = name;
        this.basePrice = basePrice;
        this.stepSize = stepSize;
        this.stepIncrement = stepIncrement;
        this.interpretation = interpretation == null ? Interpretation.TOTAL : interpretation;
        this.stepBoundary = stepBoundary == null ? StepBoundary.EXCLUSIVE : stepBoundary;
    }

    public StepFunctionCalculator(String name, Money basePrice, BigDecimal stepSize, BigDecimal stepIncrement) {
        this(CalculatorId.generate(), name, basePrice, stepSize, stepIncrement, Interpretation.TOTAL, null);
    }

    public StepFunctionCalculator(
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            Interpretation interpretation) {
        this(CalculatorId.generate(), name, basePrice, stepSize, stepIncrement, interpretation, null);
    }

    public StepFunctionCalculator(
            String name,
            Money basePrice,
            BigDecimal stepSize,
            BigDecimal stepIncrement,
            @Nullable Interpretation interpretation,
            @Nullable StepBoundary stepBoundary) {
        this(CalculatorId.generate(), name, basePrice, stepSize, stepIncrement, interpretation, stepBoundary);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException("StepFunctionCalculator requires %s parameters"
                    .formatted(getType().requiredCalculationFields()));
        }

        BigDecimal totalIncrementValue = calculateTotalIncrementValue(parameters);
        Money incrementTotal = Money.of(totalIncrementValue, basePrice.currency());

        return basePrice.add(incrementTotal);
    }

    private BigDecimal calculateTotalIncrementValue(Parameters parameters) {
        BigDecimal quantity = parameters.getBigDecimal("quantity");

        BigDecimal steps;
        if (stepBoundary == StepBoundary.INCLUSIVE && quantity.compareTo(BigDecimal.ZERO) > 0) {
            steps = quantity.subtract(BigDecimal.ONE).divide(stepSize, 0, RoundingMode.DOWN);
        } else {
            steps = quantity.divide(stepSize, 0, RoundingMode.DOWN);
        }

        return stepIncrement.multiply(steps).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    @Override
    public String describe() {
        return String.format(
                "Step function calculator - base price %s + increments every %s units", basePrice, stepSize);
    }

    @Override
    public String formula() {
        return ("f(quantity) = basePrice + ⌊quantity/%s⌋ × %s%n" + "where basePrice = %s")
                .formatted(
                        stepSize.stripTrailingZeros().toPlainString(),
                        stepIncrement.stripTrailingZeros().toPlainString(),
                        basePrice);
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.STEP_FUNCTION;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Calculator that looks up prices from predefined quantity-price pairs. */
record DiscretePointsCalculator(
        CalculatorId id, String name, Map<BigDecimal, Money> points, Interpretation interpretation)
        implements Calculator {

    public DiscretePointsCalculator(String name, Map<BigDecimal, Money> points) {
        this(CalculatorId.generate(), name, new HashMap<>(points), Interpretation.TOTAL);
    }

    public DiscretePointsCalculator(String name, Map<BigDecimal, Money> points, Interpretation interpretation) {
        this(CalculatorId.generate(), name, new HashMap<>(points), interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException("DiscretePointsCalculator requires %s parameters"
                    .formatted(getType().requiredCalculationFields()));
        }

        BigDecimal quantity = parameters.getBigDecimal("quantity");

        Money price = points.get(quantity);
        if (price == null) {
            throw new IllegalArgumentException(
                    "Quantity %s is not defined in the price points. Available quantities: %s"
                            .formatted(quantity, points.keySet()));
        }

        return price;
    }

    @Override
    public String describe() {
        return String.format("Discrete points calculator with %d price points: %s", points.size(), points);
    }

    @Override
    public String formula() {
        StringBuilder sb = new StringBuilder("f(quantity) = lookup(quantity)%nDefined points:%n".formatted());
        points.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append("  quantity = %s → %s%n"
                        .formatted(e.getKey().stripTrailingZeros().toPlainString(), e.getValue())));
        return sb.toString().trim();
    }

    @Override
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.DISCRETE_POINTS;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

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

    public DailyIncrementCalculator(String name, LocalDate startDate, Money startPrice, Money dailyIncrement) {
        this(CalculatorId.generate(), name, startDate, startPrice, dailyIncrement, Interpretation.TOTAL);
    }

    public DailyIncrementCalculator(
            String name, LocalDate startDate, Money startPrice, Money dailyIncrement, Interpretation interpretation) {
        this(CalculatorId.generate(), name, startDate, startPrice, dailyIncrement, interpretation);
    }

    @Override
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException("DailyIncrementCalculator requires %s parameters"
                    .formatted(getType().requiredCalculationFields()));
        }

        LocalDate date = parameters.getLocalDate("date");

        long daysFromStart = DAYS.between(startDate, date);

        BigDecimal daysDecimal = BigDecimal.valueOf(daysFromStart);
        Money totalIncrement = dailyIncrement.multiply(daysDecimal);

        return startPrice.add(totalIncrement);
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
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.DAILY_INCREMENT;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

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
    public Money calculate(Parameters parameters) {
        if (!parameters.containsAll(getType().requiredCalculationFields())) {
            throw new IllegalArgumentException("ContinuousLinearTimeCalculator requires %s parameters"
                    .formatted(getType().requiredCalculationFields()));
        }

        Instant queryTime = parameters.getInstant("time");

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

        return startPrice.add(interpolatedIncrease);
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
    public Interpretation interpretation() {
        return interpretation;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.CONTINUOUS_LINEAR_TIME;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Selects a calculator based on a numeric, time, or date range.
 *
 * <p>Each range delegates to one calculator registered in the repository.
 */
record CompositeFunctionCalculator(CalculatorId id, String name, Ranges ranges, CalculatorRepository repository)
        implements Calculator {

    public CompositeFunctionCalculator(String name, Ranges ranges, CalculatorRepository repository) {
        this(CalculatorId.generate(), name, ranges, repository);
    }

    public CompositeFunctionCalculator {
        validateUniformInterpretation(ranges, repository);
    }

    private static void validateUniformInterpretation(Ranges ranges, CalculatorRepository repository) {
        var calculatorIds =
                ranges.toList().stream().map(CalculatorRange::calculatorId).collect(Collectors.toSet());

        if (calculatorIds.isEmpty()) {
            throw new IllegalArgumentException("Composite calculator must have at least one range");
        }

        var calculators = repository.findByIds(calculatorIds);

        if (calculators.size() != calculatorIds.size()) {
            var foundIds = calculators.stream().map(Calculator::getId).toList();
            var missingIds =
                    calculatorIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException("Calculators not found in repository: %s".formatted(missingIds));
        }

        var interpretations =
                calculators.stream().map(Calculator::interpretation).distinct().toList();

        if (interpretations.size() > 1) {
            throw new IllegalArgumentException(
                    "All component calculators in composite must have the same interpretation. Found: %s"
                            .formatted(calculators.stream()
                                    .map(calc -> calc.name() + ":" + calc.interpretation())
                                    .toList()));
        }
    }

    @Override
    public Interpretation interpretation() {
        return ranges.toList().stream()
                .findFirst()
                .map(CalculatorRange::calculatorId)
                .flatMap(repository::findById)
                .map(Calculator::interpretation)
                .orElse(Interpretation.TOTAL);
    }

    @Override
    public Money calculate(Parameters parameters) {
        CalculatorRange matchingRange = ranges.findMatching(parameters)
                .orElseThrow(() -> new IllegalArgumentException("No matching range found in %s".formatted(ranges)));

        Calculator calculator = repository
                .findById(matchingRange.calculatorId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Calculator '%s' not found in repository".formatted(matchingRange.calculatorId())));

        return calculator.calculate(parameters);
    }

    @Override
    public String describe() {
        return String.format("Composite function calculator: %s", ranges);
    }

    @Override
    public String formula() {
        StringBuilder sb = new StringBuilder("f(x) = piecewise function:%n".formatted());
        ranges.toList().forEach(range -> {
            Calculator calc = repository
                    .findById(range.calculatorId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Calculator '%s' not found".formatted(range.calculatorId())));
            sb.append("  %s → %s: %s%n"
                    .formatted(range.describe(), calc.name(), calc.formula().replaceAll("\\R", " ")));
        });
        return sb.toString().trim();
    }

    @Override
    public CalculatorType getType() {
        return COMPOSITE;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a unit price to a total price by multiplying it by the quantity. */
record UnitToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    public static UnitToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.UNIT) {
            throw new IllegalArgumentException(
                    "UnitToTotalAdapter requires UNIT calculator, got: " + sourceCalculator.interpretation());
        }
        return new UnitToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.UNIT_TO_TOTAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");
        Money unitPrice = sourceCalculator.calculate(params);
        return unitPrice.multiply(quantity);
    }

    @Override
    public String formula() {
        return "quantity × (" + sourceCalculator.formula() + ")";
    }

    @Override
    public String describe() {
        return "Unit to Total: " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Converts a unit price to a marginal price using the difference between consecutive totals.
 *
 * <p>Works with both constant and variable unit prices.
 */
record UnitToMarginalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    public static UnitToMarginalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.UNIT) {
            throw new IllegalArgumentException(
                    "UnitToMarginalAdapter requires UNIT calculator, got: " + sourceCalculator.interpretation());
        }
        return new UnitToMarginalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.UNIT_TO_MARGINAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Marginal price requires quantity >= 1");
        }

        Money unitPriceN = sourceCalculator.calculate(params);
        Money totalN = unitPriceN.multiply(quantity);

        if (quantity.compareTo(BigDecimal.ONE) == 0) {
            return totalN;
        }

        BigDecimal quantityMinusOne = quantity.subtract(BigDecimal.ONE);
        Parameters paramsN1 = Parameters.of("quantity", quantityMinusOne);
        Money unitPriceN1 = sourceCalculator.calculate(paramsN1);
        Money totalN1 = unitPriceN1.multiply(quantityMinusOne);

        return totalN.subtract(totalN1);
    }

    @Override
    public String formula() {
        return "marginal(n) = (unit(n) × n) - (unit(n-1) × (n-1)) where unit = " + sourceCalculator.formula();
    }

    @Override
    public String describe() {
        return "Unit to Marginal (derivative): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.MARGINAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a total price to an average unit price by dividing it by the quantity. */
record TotalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    public static TotalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.TOTAL) {
            throw new IllegalArgumentException(
                    "TotalToUnitAdapter requires TOTAL calculator, got: " + sourceCalculator.interpretation());
        }
        return new TotalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.TOTAL_TO_UNIT_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");
        Money total = sourceCalculator.calculate(params);
        return total.divide(quantity);
    }

    @Override
    public String formula() {
        return "(" + sourceCalculator.formula() + ") / quantity";
    }

    @Override
    public String describe() {
        return "Total to Unit (average): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a total price to a marginal price using the difference between consecutive totals. */
record TotalToMarginalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    public static TotalToMarginalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.TOTAL) {
            throw new IllegalArgumentException(
                    "TotalToMarginalAdapter requires TOTAL calculator, got: " + sourceCalculator.interpretation());
        }
        return new TotalToMarginalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.TOTAL_TO_MARGINAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Marginal price requires quantity >= 1");
        }

        Money totalN = sourceCalculator.calculate(params);

        if (quantity.compareTo(BigDecimal.ONE) == 0) {
            return totalN;
        }

        BigDecimal quantityMinusOne = quantity.subtract(BigDecimal.ONE);
        Parameters paramsN1 = Parameters.of("quantity", quantityMinusOne);
        Money totalN1 = sourceCalculator.calculate(paramsN1);

        return totalN.subtract(totalN1);
    }

    @Override
    public String formula() {
        return "marginal(n) = total(n) - total(n-1) where total = " + sourceCalculator.formula();
    }

    @Override
    public String describe() {
        return "Total to Marginal (derivative): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.MARGINAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a marginal price to a total price by summing marginal prices from one through the quantity. */
record MarginalToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    public static MarginalToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.MARGINAL) {
            throw new IllegalArgumentException(
                    "MarginalToTotalAdapter requires MARGINAL calculator, got: " + sourceCalculator.interpretation());
        }
        return new MarginalToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.MARGINAL_TO_TOTAL_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        Money total = sourceCalculator.calculate(Parameters.of("quantity", BigDecimal.ONE));

        for (int i = 2; i <= quantity.intValue(); i++) {
            Parameters marginalParams = Parameters.of("quantity", new BigDecimal(i));
            Money marginal = sourceCalculator.calculate(marginalParams);
            total = total.add(marginal);
        }

        return total;
    }

    @Override
    public String formula() {
        return "Σ[i=1→q] (" + sourceCalculator.formula() + ")";
    }

    @Override
    public String describe() {
        return "Marginal to Total (sum): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a marginal price to an average unit price by summing marginal prices and dividing by the quantity. */
record MarginalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    public static MarginalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        if (sourceCalculator.interpretation() != Interpretation.MARGINAL) {
            throw new IllegalArgumentException(
                    "MarginalToUnitAdapter requires MARGINAL calculator, got: " + sourceCalculator.interpretation());
        }
        return new MarginalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.MARGINAL_TO_UNIT_ADAPTER;
    }

    @Override
    public Money calculate(Parameters params) {
        BigDecimal quantity = params.getBigDecimal("quantity");

        Money total = sourceCalculator.calculate(Parameters.of("quantity", BigDecimal.ONE));

        for (int i = 2; i <= quantity.intValue(); i++) {
            Parameters marginalParams = Parameters.of("quantity", new BigDecimal(i));
            Money marginal = sourceCalculator.calculate(marginalParams);
            total = total.add(marginal);
        }

        return total.divide(quantity);
    }

    @Override
    public String formula() {
        return "(Σ[i=1→q] (" + sourceCalculator.formula() + ")) / quantity";
    }

    @Override
    public String describe() {
        return "Marginal to Unit (average): " + sourceCalculator.describe();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.UNIT;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

/** Calculates a configured percentage of the {@code baseAmount} parameter. */
record PercentageCalculator(CalculatorId id, String name, BigDecimal percentageRate) implements Calculator {

    public PercentageCalculator(String name, BigDecimal percentageRate) {
        this(CalculatorId.generate(), name, percentageRate);
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.PERCENTAGE;
    }

    @Override
    public Money calculate(Parameters params) {
        Money baseAmount = params.getMoney("baseAmount");
        BigDecimal rate = percentageRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        Money result = baseAmount.multiply(rate);
        return Money.of(result.value().setScale(2, RoundingMode.HALF_UP), result.currency());
    }

    @Override
    public String formula() {
        return "baseAmount × " + percentageRate + "%";
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
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

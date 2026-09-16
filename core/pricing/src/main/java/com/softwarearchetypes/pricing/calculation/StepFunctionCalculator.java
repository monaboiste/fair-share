package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.jspecify.annotations.Nullable;

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

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

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
    public PricingResult calculateWithValidInputs(Parameters parameters) {

        BigDecimal totalIncrementValue = calculateTotalIncrementValue(parameters);
        Money incrementTotal = Money.of(totalIncrementValue, basePrice.currency());

        return Calculator.result(interpretation, basePrice.add(incrementTotal));
    }

    private BigDecimal calculateTotalIncrementValue(Parameters parameters) {
        BigDecimal quantity = parameters.get(QUANTITY);

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
    public CalculatorId getId() {
        return id;
    }
}

/** Calculator that looks up prices from predefined quantity-price pairs. */

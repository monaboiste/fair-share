package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** Calculator that looks up prices from predefined quantity-price pairs. */
record DiscretePointsCalculator(
        CalculatorId id, String name, Map<BigDecimal, Money> points, Interpretation interpretation)
        implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public DiscretePointsCalculator(String name, Map<BigDecimal, Money> points) {
        this(CalculatorId.generate(), name, new HashMap<>(points), Interpretation.TOTAL);
    }

    public DiscretePointsCalculator(String name, Map<BigDecimal, Money> points, Interpretation interpretation) {
        this(CalculatorId.generate(), name, new HashMap<>(points), interpretation);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {
        BigDecimal quantity = parameters.get(QUANTITY);

        Money price = points.get(quantity);
        if (price == null) {
            throw new IllegalArgumentException(
                    "Quantity %s is not defined in the price points. Available quantities: %s"
                            .formatted(quantity, points.keySet()));
        }

        return PricingResults.of(interpretation, price);
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
    public CalculatorId getId() {
        return id;
    }
}

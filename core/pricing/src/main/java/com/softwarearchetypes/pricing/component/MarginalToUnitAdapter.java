package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;

record MarginalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static MarginalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        return new MarginalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculateWithValidInputs(Parameters params) {
        BigDecimal quantity = params.get(QUANTITY);

        PricingResult first = sourceCalculator.calculate(params.with(QUANTITY, BigDecimal.ONE));
        Money total = first.money();

        for (int i = 2; i <= quantity.intValue(); i++) {
            Parameters marginalParams = params.with(QUANTITY, new BigDecimal(i));
            PricingResult marginal = sourceCalculator.calculate(marginalParams);
            total = total.add(marginal.money());
        }

        return new UnitPrice(total.divide(quantity));
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
    public CalculatorId getId() {
        return id;
    }
}

/** Calculates a configured percentage of the {@code baseAmount} parameter. */

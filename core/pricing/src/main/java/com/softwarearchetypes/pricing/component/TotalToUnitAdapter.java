package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Calculator;
import com.softwarearchetypes.pricing.calculation.CalculatorId;
import com.softwarearchetypes.pricing.calculation.ParameterKey;
import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.pricing.calculation.UnitPrice;
import java.math.BigDecimal;

record TotalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static TotalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        return new TotalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculateWithValidInputs(Parameters params) {
        BigDecimal quantity = params.get(QUANTITY);
        PricingResult total = sourceCalculator.calculate(params);
        return new UnitPrice(total.money().divide(quantity));
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
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a total price to a marginal price using the difference between consecutive totals. */

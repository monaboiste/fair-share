package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Calculator;
import com.softwarearchetypes.pricing.calculation.CalculatorId;
import com.softwarearchetypes.pricing.calculation.ParameterKey;
import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.pricing.calculation.TotalPrice;
import java.math.BigDecimal;

record UnitToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static UnitToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        return new UnitToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculateWithValidInputs(Parameters params) {
        BigDecimal quantity = params.get(QUANTITY);
        PricingResult unitPrice = sourceCalculator.calculate(params);
        return new TotalPrice(unitPrice.money().multiply(quantity));
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
    public CalculatorId getId() {
        return id;
    }
}

/**
 * Converts a unit price to a marginal price using the difference between consecutive totals.
 *
 * <p>Works with both constant and variable unit prices.
 */

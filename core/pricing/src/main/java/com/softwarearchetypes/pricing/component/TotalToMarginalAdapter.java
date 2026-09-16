package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Calculator;
import com.softwarearchetypes.pricing.calculation.CalculatorId;
import com.softwarearchetypes.pricing.calculation.MarginalPrice;
import com.softwarearchetypes.pricing.calculation.ParameterKey;
import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import java.math.BigDecimal;

record TotalToMarginalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static TotalToMarginalAdapter wrap(String name, Calculator sourceCalculator) {
        return new TotalToMarginalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculateWithValidInputs(Parameters params) {
        BigDecimal quantity = params.get(QUANTITY);

        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Marginal price requires quantity >= 1");
        }

        PricingResult totalN = sourceCalculator.calculate(params);

        if (quantity.compareTo(BigDecimal.ONE) == 0) {
            return new MarginalPrice(totalN.money());
        }

        BigDecimal quantityMinusOne = quantity.subtract(BigDecimal.ONE);
        Parameters paramsN1 = params.with(QUANTITY, quantityMinusOne);
        PricingResult totalN1 = sourceCalculator.calculate(paramsN1);

        return new MarginalPrice(totalN.money().subtract(totalN1.money()));
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
    public CalculatorId getId() {
        return id;
    }
}

/** Converts a marginal price to a total price by summing marginal prices from one through the quantity. */

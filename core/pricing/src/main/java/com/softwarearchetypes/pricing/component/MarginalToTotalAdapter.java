package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Calculator;
import com.softwarearchetypes.pricing.calculation.CalculatorId;
import com.softwarearchetypes.pricing.calculation.ParameterKey;
import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.pricing.calculation.TotalPrice;
import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;

record MarginalToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static MarginalToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        return new MarginalToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculate(Parameters params) {
        BigDecimal quantity = params.get(QUANTITY);

        PricingResult first = sourceCalculator.calculate(params.with(QUANTITY, BigDecimal.ONE));
        Money total = first.money();

        for (int i = 2; i <= quantity.intValue(); i++) {
            Parameters marginalParams = params.with(QUANTITY, new BigDecimal(i));
            PricingResult marginal = sourceCalculator.calculate(marginalParams);
            total = total.add(marginal.money());
        }

        return new TotalPrice(total);
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
    public CalculatorId getId() {
        return id;
    }
}

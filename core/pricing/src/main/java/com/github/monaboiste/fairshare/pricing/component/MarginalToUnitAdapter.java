package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId;
import com.github.monaboiste.fairshare.pricing.calculation.ParameterKey;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.pricing.calculation.UnitPrice;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;

record MarginalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static MarginalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        return new MarginalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
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

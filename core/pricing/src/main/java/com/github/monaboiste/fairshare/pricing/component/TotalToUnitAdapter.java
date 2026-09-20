package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId;
import com.github.monaboiste.fairshare.pricing.calculation.ParameterKey;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.pricing.calculation.UnitPrice;
import java.math.BigDecimal;

record TotalToUnitAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static TotalToUnitAdapter wrap(String name, Calculator sourceCalculator) {
        return new TotalToUnitAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculate(Parameters params) {
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

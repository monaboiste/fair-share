package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId;
import com.github.monaboiste.fairshare.pricing.calculation.ParameterKey;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.pricing.calculation.TotalPrice;
import java.math.BigDecimal;

record UnitToTotalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static UnitToTotalAdapter wrap(String name, Calculator sourceCalculator) {
        return new UnitToTotalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculate(Parameters params) {
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

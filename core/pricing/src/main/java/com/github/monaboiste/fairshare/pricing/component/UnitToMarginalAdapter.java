package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId;
import com.github.monaboiste.fairshare.pricing.calculation.MarginalPrice;
import com.github.monaboiste.fairshare.pricing.calculation.ParameterKey;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;

record UnitToMarginalAdapter(CalculatorId id, String name, Calculator sourceCalculator) implements Calculator {

    private static final ParameterKey<BigDecimal> QUANTITY = new ParameterKey<>("quantity", BigDecimal.class);

    public static UnitToMarginalAdapter wrap(String name, Calculator sourceCalculator) {
        return new UnitToMarginalAdapter(CalculatorId.generate(), name, sourceCalculator);
    }

    @Override
    public PricingResult calculate(Parameters params) {
        BigDecimal quantity = params.get(QUANTITY);

        if (quantity.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Marginal price requires quantity >= 1");
        }

        PricingResult unitPriceN = sourceCalculator.calculate(params);
        Money totalN = unitPriceN.money().multiply(quantity);

        if (quantity.compareTo(BigDecimal.ONE) == 0) {
            return new MarginalPrice(totalN);
        }

        BigDecimal quantityMinusOne = quantity.subtract(BigDecimal.ONE);
        Parameters paramsN1 = params.with(QUANTITY, quantityMinusOne);
        PricingResult unitPriceN1 = sourceCalculator.calculate(paramsN1);
        Money totalN1 = unitPriceN1.money().multiply(quantityMinusOne);

        return new MarginalPrice(totalN.subtract(totalN1));
    }

    @Override
    public String formula() {
        return "marginal(n) = (unit(n) × n) - (unit(n-1) × (n-1)) where unit = " + sourceCalculator.formula();
    }

    @Override
    public String describe() {
        return "Unit to Marginal (derivative): " + sourceCalculator.describe();
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

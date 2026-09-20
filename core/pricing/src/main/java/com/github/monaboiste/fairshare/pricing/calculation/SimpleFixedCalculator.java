package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;

record SimpleFixedCalculator(CalculatorId id, String name, Money amount, Interpretation interpretation)
        implements Calculator {

    public SimpleFixedCalculator(String name, Money amount) {
        this(CalculatorId.generate(), name, amount, Interpretation.TOTAL);
    }

    public SimpleFixedCalculator(String name, Money amount, Interpretation interpretation) {
        this(CalculatorId.generate(), name, amount, interpretation);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {
        return PricingResults.of(interpretation, amount);
    }

    @Override
    public String describe() {
        return "Fixed amount calculator - returns " + amount + " regardless";
    }

    @Override
    public String formula() {
        return "f(x) = %s".formatted(amount);
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}

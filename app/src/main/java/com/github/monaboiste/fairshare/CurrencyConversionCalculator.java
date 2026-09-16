package com.github.monaboiste.fairshare;

import com.softwarearchetypes.pricing.Calculator;
import com.softwarearchetypes.pricing.CalculatorId;
import com.softwarearchetypes.pricing.CalculatorType;
import com.softwarearchetypes.pricing.Interpretation;
import com.softwarearchetypes.pricing.ParameterKey;
import com.softwarearchetypes.pricing.Parameters;
import com.softwarearchetypes.pricing.PricingResult;
import com.softwarearchetypes.pricing.TotalPrice;
import com.softwarearchetypes.quantity.money.Money;

record CurrencyConversionCalculator(CalculatorId id, ExchangeRate exchangeRate) implements Calculator {

    CurrencyConversionCalculator(ExchangeRate exchangeRate) {
        this(CalculatorId.generate(), exchangeRate);
    }

    private static final ParameterKey<Money> SOURCE = new ParameterKey<>("source", Money.class);

    @Override
    public PricingResult calculateWithValidInputs(Parameters parameters) {
        return new TotalPrice(exchangeRate.convert(parameters.get(SOURCE)));
    }

    @Override
    public String describe() {
        return "Currency conversion using Exchange Rate " + exchangeRate;
    }

    @Override
    public String formula() {
        return "source × " + exchangeRate.value();
    }

    @Override
    public Interpretation interpretation() {
        return Interpretation.TOTAL;
    }

    @Override
    public CalculatorType getType() {
        return CalculatorType.CUSTOM;
    }

    @Override
    public CalculatorId getId() {
        return id;
    }

    @Override
    public String name() {
        return "currency-conversion";
    }
}

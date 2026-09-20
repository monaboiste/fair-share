package com.github.monaboiste.fairshare;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId;
import com.github.monaboiste.fairshare.pricing.calculation.ParameterKey;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.pricing.calculation.TotalPrice;
import com.github.monaboiste.fairshare.quantity.money.Money;

record CurrencyConversionCalculator(CalculatorId id, ExchangeRate exchangeRate) implements Calculator {

    CurrencyConversionCalculator(ExchangeRate exchangeRate) {
        this(CalculatorId.generate(), exchangeRate);
    }

    private static final ParameterKey<Money> SOURCE = new ParameterKey<>("source", Money.class);

    @Override
    public PricingResult calculate(Parameters parameters) {
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
    public CalculatorId getId() {
        return id;
    }

    @Override
    public String name() {
        return "currency-conversion";
    }
}

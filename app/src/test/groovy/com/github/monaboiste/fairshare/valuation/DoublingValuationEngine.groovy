package com.github.monaboiste.fairshare.valuation

import com.github.monaboiste.fairshare.pricing.calculation.TotalPrice
import com.github.monaboiste.fairshare.quantity.money.Money

import javax.money.CurrencyUnit

class DoublingValuationEngine implements ValuationEngine {
    private final ValuationEngine standard = ValuationEngine.standard()

    @Override
    Valuation identity(Money source) {
        doubled(standard.identity(source))
    }

    @Override
    Valuation value(Money source, CurrencyUnit target, ExchangeRateVersion selected) {
        doubled(standard.value(source, target, selected))
    }

    @Override
    Valuation value(Money source, CurrencyUnit target, ExchangeRateOverride override) {
        doubled(standard.value(source, target, override))
    }

    private static Valuation doubled(Valuation valuation) {
        new Valuation(new TotalPrice(valuation.money().multiply(2)), valuation.exchangeRate(),
                valuation.componentVersion())
    }
}

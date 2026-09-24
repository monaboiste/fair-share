package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingContext;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import javax.money.CurrencyUnit;

/** Enforces {@link PricingContext#CURRENCY} at the component boundary. */
final class EvaluationCurrency {

    private EvaluationCurrency() {}

    /** Returns the required evaluation currency. */
    static CurrencyUnit of(Parameters parameters) {
        return parameters.get(PricingContext.CURRENCY);
    }

    /** Returns the result when it is denominated in the evaluation currency. */
    static PricingResult expect(String component, CurrencyUnit currency, PricingResult result) {
        if (!result.money().currencyUnit().equals(currency)) {
            throw new IllegalStateException("Component '%s' produced %s in an evaluation in %s"
                    .formatted(component, result.money(), currency.getCurrencyCode()));
        }
        return result;
    }
}

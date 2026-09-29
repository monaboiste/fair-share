package com.github.monaboiste.fairshare.valuation;

import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult;
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.pricing.component.SimpleComponentVersion;
import com.github.monaboiste.fairshare.pricing.component.Validity;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.money.CurrencyUnit;

public interface ValuationEngine {

    Valuation identity(Money source);

    Valuation value(Money source, CurrencyUnit target, ExchangeRateVersion selected);

    Valuation value(Money source, CurrencyUnit target, ExchangeRateOverride override);

    static ValuationEngine standard() {
        return new StandardValuationEngine();
    }
}

final class StandardValuationEngine implements ValuationEngine {

    @Override
    public Valuation identity(Money source) {
        CurrencyUnit targetCurrency = source.currencyUnit();
        ExchangeRate identity = ExchangeRate.of(targetCurrency, targetCurrency, BigDecimal.ONE);
        String identityName = "implicit-exchange-rate:" + targetCurrency.getCurrencyCode();
        ComponentVersionId identityId =
                new ComponentVersionId(UUID.nameUUIDFromBytes(identityName.getBytes(StandardCharsets.UTF_8)));
        return value(source, targetCurrency, identity.version(identityId, Validity.always(), LocalDateTime.MIN));
    }

    @Override
    public Valuation value(Money source, CurrencyUnit targetCurrency, ExchangeRateVersion selected) {
        return value(
                source,
                targetCurrency,
                selected.exchangeRate().version(selected.id(), selected.validity(), selected.definedAt()));
    }

    @Override
    public Valuation value(Money source, CurrencyUnit targetCurrency, ExchangeRateOverride override) {
        return value(
                source,
                targetCurrency,
                override.rate().version(override.versionId(), Validity.always(), override.definedAt()));
    }

    private Valuation value(Money source, CurrencyUnit targetCurrency, SimpleComponentVersion version) {
        CurrencyConversionCalculator calculator = (CurrencyConversionCalculator) version.calculator();
        ExchangeRate exchangeRate = calculator.exchangeRate();
        if (!exchangeRate.sourceCurrency().equals(source.currencyUnit())
                || !exchangeRate.targetCurrency().equals(targetCurrency)) {
            throw new IllegalArgumentException("Exchange Rate direction does not match the Valuation currencies");
        }
        PricingResult result = calculator.calculate(Parameters.of("source", source));
        return new Valuation(result, exchangeRate, version);
    }
}

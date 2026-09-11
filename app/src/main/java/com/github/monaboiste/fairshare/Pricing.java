package com.github.monaboiste.fairshare;

import com.softwarearchetypes.pricing.ComponentVersionId;
import com.softwarearchetypes.pricing.Parameters;
import com.softwarearchetypes.pricing.SimpleComponentVersion;
import com.softwarearchetypes.pricing.Validity;
import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public interface Pricing {

    Valuation value(Money source, CurrencyUnit targetCurrency, LocalDateTime at, List<SimpleComponentVersion> versions);

    Valuation value(
            Money source,
            CurrencyUnit targetCurrency,
            LocalDateTime at,
            ExchangeRate override,
            ComponentVersionId versionId);

    static Pricing standard() {
        return new StandardPricing();
    }
}

final class StandardPricing implements Pricing {

    @Override
    public Valuation value(
            Money source, CurrencyUnit targetCurrency, LocalDateTime at, List<SimpleComponentVersion> versions) {
        if (source.currencyUnit().equals(targetCurrency)) {
            ExchangeRate identity = ExchangeRate.of(targetCurrency, targetCurrency, BigDecimal.ONE);
            String identityName = "implicit-exchange-rate:" + targetCurrency.getCurrencyCode();
            ComponentVersionId identityId =
                    new ComponentVersionId(UUID.nameUUIDFromBytes(identityName.getBytes(StandardCharsets.UTF_8)));
            SimpleComponentVersion identityVersion = identity.version(identityId, Validity.always(), LocalDateTime.MIN);
            return value(source, targetCurrency, identityVersion);
        }
        return value(source, targetCurrency, versionAt(source.currencyUnit(), targetCurrency, at, versions));
    }

    @Override
    public Valuation value(
            Money source,
            CurrencyUnit targetCurrency,
            LocalDateTime at,
            ExchangeRate override,
            ComponentVersionId versionId) {
        return value(source, targetCurrency, override.version(versionId, Validity.always(), at));
    }

    private Valuation value(Money source, CurrencyUnit targetCurrency, SimpleComponentVersion version) {
        CurrencyConversionCalculator calculator = (CurrencyConversionCalculator) version.calculator();
        ExchangeRate exchangeRate = calculator.exchangeRate();
        if (!exchangeRate.sourceCurrency().equals(source.currencyUnit())
                || !exchangeRate.targetCurrency().equals(targetCurrency)) {
            throw new IllegalArgumentException("Exchange Rate direction does not match the Valuation currencies");
        }
        Money amount = calculator.calculate(Parameters.of("source", source));
        return new Valuation(amount, exchangeRate, version);
    }

    private SimpleComponentVersion versionAt(
            CurrencyUnit sourceCurrency,
            CurrencyUnit targetCurrency,
            LocalDateTime at,
            List<SimpleComponentVersion> versions) {
        SimpleComponentVersion selected = null;
        Comparator<@Nullable LocalDateTime> validFrom = Comparator.nullsFirst(Comparator.naturalOrder());
        for (SimpleComponentVersion version : versions) {
            if (version.validity().isValidAt(at)
                    && version.calculator() instanceof CurrencyConversionCalculator calculator
                    && calculator.exchangeRate().sourceCurrency().equals(sourceCurrency)
                    && calculator.exchangeRate().targetCurrency().equals(targetCurrency)
                    && (selected == null
                            || validFrom.compare(
                                            version.validity().from(),
                                            selected.validity().from())
                                    >= 0)) {
                selected = version;
            }
        }
        if (selected == null) {
            throw new IllegalStateException("No Exchange Rate version applies at " + at);
        }
        return selected;
    }
}

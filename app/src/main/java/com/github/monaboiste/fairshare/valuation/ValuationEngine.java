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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public interface ValuationEngine {

    Optional<Valuation> value(
            Money source, CurrencyUnit targetCurrency, LocalDateTime at, List<ExchangeRateVersion> versions);

    Valuation value(
            Money source,
            CurrencyUnit targetCurrency,
            LocalDateTime at,
            ExchangeRate override,
            ComponentVersionId versionId);

    static ValuationEngine standard() {
        return new StandardValuationEngine();
    }
}

final class StandardValuationEngine implements ValuationEngine {

    @Override
    public Optional<Valuation> value(
            Money source, CurrencyUnit targetCurrency, LocalDateTime at, List<ExchangeRateVersion> versions) {
        if (source.currencyUnit().equals(targetCurrency)) {
            ExchangeRate identity = ExchangeRate.of(targetCurrency, targetCurrency, BigDecimal.ONE);
            String identityName = "implicit-exchange-rate:" + targetCurrency.getCurrencyCode();
            ComponentVersionId identityId =
                    new ComponentVersionId(UUID.nameUUIDFromBytes(identityName.getBytes(StandardCharsets.UTF_8)));
            SimpleComponentVersion identityVersion = identity.version(identityId, Validity.always(), LocalDateTime.MIN);
            return Optional.of(value(source, targetCurrency, identityVersion));
        }
        return versionAt(source.currencyUnit(), targetCurrency, at, versions)
                .map(selected -> value(
                        source,
                        targetCurrency,
                        selected.exchangeRate().version(selected.id(), selected.validity(), selected.definedAt())));
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
        PricingResult result = calculator.calculate(Parameters.of("source", source));
        return new Valuation(result, exchangeRate, version);
    }

    private Optional<ExchangeRateVersion> versionAt(
            CurrencyUnit sourceCurrency,
            CurrencyUnit targetCurrency,
            LocalDateTime at,
            List<ExchangeRateVersion> versions) {
        ExchangeRateVersion selected = null;
        Comparator<@Nullable LocalDateTime> validFrom = Comparator.nullsFirst(Comparator.naturalOrder());
        for (ExchangeRateVersion version : versions) {
            if (version.validity().isValidAt(at)
                    && version.exchangeRate().sourceCurrency().equals(sourceCurrency)
                    && version.exchangeRate().targetCurrency().equals(targetCurrency)
                    && (selected == null
                            || validFrom.compare(
                                            version.validity().from(),
                                            selected.validity().from())
                                    >= 0)) {
                selected = version;
            }
        }
        return Optional.ofNullable(selected);
    }
}

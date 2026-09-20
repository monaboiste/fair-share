package com.github.monaboiste.fairshare.valuation;

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.pricing.component.SimpleComponentVersion;
import com.github.monaboiste.fairshare.pricing.component.Validity;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import javax.money.CurrencyUnit;

public record ExchangeRate(CurrencyUnit sourceCurrency, CurrencyUnit targetCurrency, BigDecimal value) {

    public ExchangeRate {
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Exchange Rate must be positive");
        }
        if (value.scale() > 12) {
            throw new IllegalArgumentException("Exchange Rate cannot exceed twelve fractional digits");
        }
    }

    public static ExchangeRate of(CurrencyUnit sourceCurrency, CurrencyUnit targetCurrency, BigDecimal value) {
        return new ExchangeRate(sourceCurrency, targetCurrency, value);
    }

    public SimpleComponentVersion version(ComponentVersionId id, Validity validity, LocalDateTime definedAt) {
        return SimpleComponentVersion.of(id, new CurrencyConversionCalculator(this), validity, definedAt);
    }

    /**
     * Converts the complete source amount and rounds once to the target currency's fraction digits using
     * {@link RoundingMode#HALF_UP}.
     *
     * @param money source amount in this Exchange Rate's source currency
     * @return converted amount in the target currency
     * @throws IllegalArgumentException when the source currency does not match
     */
    public Money convert(Money money) {
        if (!money.currencyUnit().equals(sourceCurrency)) {
            throw new IllegalArgumentException("Conversion failure: expected %s, got %s"
                    .formatted(sourceCurrency.getCurrencyCode(), money.currency()));
        }
        BigDecimal converted =
                money.value().multiply(value).setScale(targetCurrency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return Money.of(converted, targetCurrency.getCurrencyCode());
    }
}

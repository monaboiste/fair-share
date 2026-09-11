package com.softwarearchetypes.quantity.money;

import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.money.CurrencyUnit;
import javax.money.MonetaryException;
import org.jspecify.annotations.Nullable;

public class Money implements Comparable<Money> {

    private final org.javamoney.moneta.Money value;

    private Money(org.javamoney.moneta.Money money) {
        value = money;
    }

    public static Money of(Number amount, String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(amount, currencyCode));
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(amount, currencyCode));
    }

    public static Money zero(String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(0, currencyCode));
    }

    public static Money min(Money lhs, Money rhs) {
        return lhs.compareTo(rhs) <= 0 ? lhs : rhs;
    }

    public static Optional<Money> min(Set<Money> values) {
        return values.stream().reduce(Money::min);
    }

    public static Money max(Money lhs, Money rhs) {
        return lhs.compareTo(rhs) <= 0 ? rhs : lhs;
    }

    public Money add(Money toAdd) {
        return new Money(value.add(toAdd.value));
    }

    public Money subtract(Money toSubtract) {
        return new Money(value.subtract(toSubtract.value));
    }

    public Money negate() {
        return new Money(value.negate());
    }

    public Money abs() {
        return new Money(value.abs());
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(value.multiply(multiplier));
    }

    public Money multiply(Number multiplier) {
        return new Money(value.multiply(multiplier));
    }

    public Money divide(BigDecimal divisor) {
        return new Money(value.divide(divisor));
    }

    public Money divide(BigDecimal divisor, RoundingMode roundingMode) {
        BigDecimal result = value().divide(divisor, currencyUnit().getDefaultFractionDigits(), roundingMode);
        return Money.of(result, currency());
    }

    public Money round(RoundingMode roundingMode) {
        return Money.of(value().setScale(currencyUnit().getDefaultFractionDigits(), roundingMode), currency());
    }

    public Money[] divideAndRemainder(BigDecimal divider) {
        if (divider.signum() < 0 || divider.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Divider must be a positive whole number");
        }
        BigDecimal quotient = value().divide(divider, currencyUnit().getDefaultFractionDigits(), RoundingMode.DOWN);
        BigDecimal remainder = value().subtract(quotient.multiply(divider));
        return new Money[] {Money.of(quotient, currency()), Money.of(remainder, currency())};
    }

    public Money multiply(Percentage percentage) {
        BigDecimal multiplier = percentage.value().divide(new BigDecimal(100), 30, HALF_UP);
        return multiply(multiplier);
    }

    public boolean isZero() {
        return value.isZero();
    }

    public boolean isNegative() {
        return value.isNegative();
    }

    public boolean isGreaterThan(Money other) {
        return value.isGreaterThan(other.value);
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return value.isGreaterThanOrEqualTo(other.value);
    }

    /**
     * Returns the monetary value as a normalized {@link BigDecimal}.
     *
     * <p>Rounds the raw value to 10 decimal places (HALF_UP) to remove floating-point artifacts, strips trailing zeros,
     * and ensures a non-negative scale to prevent scientific notation.
     *
     * @return the cleaned and formatted {@link BigDecimal} value
     */
    public BigDecimal value() {
        BigDecimal raw = value.getNumber().numberValue(BigDecimal.class);
        BigDecimal stripped = raw.setScale(10, HALF_UP).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0, HALF_UP) : stripped;
    }

    public String currency() {
        return value.getCurrency().getCurrencyCode();
    }

    @Override
    public int compareTo(Money other) {
        if (!value.getCurrency().equals(other.value.getCurrency())) {
            throw new MonetaryException(
                    "Currency mismatch: %s/%s".formatted(value.getCurrency(), other.value.getCurrency()));
        }
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value.getCurrency().getCurrencyCode() + " "
                + value.getNumberStripped().toPlainString();
    }

    public Money smallestUnit() {
        return Money.of(BigDecimal.ONE.movePointLeft(currencyUnit().getDefaultFractionDigits()), currency());
    }

    public CurrencyUnit currencyUnit() {
        return value.getCurrency();
    }
}

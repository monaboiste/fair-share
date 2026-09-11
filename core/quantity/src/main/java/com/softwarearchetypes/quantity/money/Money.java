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

/** An immutable monetary amount with currency-aware arithmetic and rounding. */
public class Money implements Comparable<Money> {

    private final org.javamoney.moneta.Money value;

    private Money(org.javamoney.moneta.Money money) {
        value = money;
    }

    /**
     * Creates an amount in the given currency.
     *
     * @param amount numeric amount
     * @param currencyCode ISO 4217 currency code
     * @return created Money
     */
    public static Money of(Number amount, String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(amount, currencyCode));
    }

    /**
     * Creates a decimal amount in the given currency.
     *
     * @param amount decimal amount
     * @param currencyCode ISO 4217 currency code
     * @return created Money
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(amount, currencyCode));
    }

    /**
     * Creates a zero amount in the given currency.
     *
     * @param currencyCode ISO 4217 currency code
     * @return zero Money
     */
    public static Money zero(String currencyCode) {
        return new Money(org.javamoney.moneta.Money.of(0, currencyCode));
    }

    /**
     * Returns the lesser of two same-currency amounts.
     *
     * @param lhs first amount
     * @param rhs second amount
     * @return lesser amount
     * @throws MonetaryException when currencies differ
     */
    public static Money min(Money lhs, Money rhs) {
        return lhs.compareTo(rhs) <= 0 ? lhs : rhs;
    }

    /**
     * Returns the least amount in a same-currency set.
     *
     * @param values candidate amounts
     * @return least amount, or empty when the set is empty
     * @throws MonetaryException when currencies differ
     */
    public static Optional<Money> min(Set<Money> values) {
        return values.stream().reduce(Money::min);
    }

    /**
     * Returns the greater of two same-currency amounts.
     *
     * @param lhs first amount
     * @param rhs second amount
     * @return greater amount
     * @throws MonetaryException when currencies differ
     */
    public static Money max(Money lhs, Money rhs) {
        return lhs.compareTo(rhs) <= 0 ? rhs : lhs;
    }

    /**
     * Adds a same-currency amount.
     *
     * @param toAdd amount to add
     * @return sum
     * @throws MonetaryException when currencies differ
     */
    public Money add(Money toAdd) {
        return new Money(value.add(toAdd.value));
    }

    /**
     * Subtracts a same-currency amount.
     *
     * @param toSubtract amount to subtract
     * @return difference
     * @throws MonetaryException when currencies differ
     */
    public Money subtract(Money toSubtract) {
        return new Money(value.subtract(toSubtract.value));
    }

    /** Returns this amount with its sign reversed. */
    public Money negate() {
        return new Money(value.negate());
    }

    /** Returns the absolute value of this amount. */
    public Money abs() {
        return new Money(value.abs());
    }

    /**
     * Multiplies this amount without rounding.
     *
     * @param multiplier decimal multiplier
     * @return product in the same currency
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(value.multiply(multiplier));
    }

    /**
     * Multiplies this amount without rounding.
     *
     * @param multiplier numeric multiplier
     * @return product in the same currency
     */
    public Money multiply(Number multiplier) {
        return new Money(value.multiply(multiplier));
    }

    /**
     * Divides this amount without applying currency-scale rounding.
     *
     * @param divisor divisor
     * @return quotient in the same currency
     */
    public Money divide(BigDecimal divisor) {
        return new Money(value.divide(divisor));
    }

    /**
     * Divides and rounds to the currency's default fraction digits.
     *
     * @param divisor divisor
     * @param roundingMode rounding mode
     * @return rounded quotient
     */
    public Money divide(BigDecimal divisor, RoundingMode roundingMode) {
        BigDecimal result = value().divide(divisor, currencyUnit().getDefaultFractionDigits(), roundingMode);
        return Money.of(result, currency());
    }

    /**
     * Rounds to the currency's default fraction digits.
     *
     * @param roundingMode rounding mode
     * @return rounded amount
     */
    public Money round(RoundingMode roundingMode) {
        return Money.of(value().setScale(currencyUnit().getDefaultFractionDigits(), roundingMode), currency());
    }

    /**
     * Divides by a positive whole number, rounding the quotient down to currency scale.
     *
     * @param divider positive whole-number divider
     * @return quotient and remainder
     * @throws IllegalArgumentException when the divider is negative or fractional
     */
    public Money[] divideAndRemainder(BigDecimal divider) {
        if (divider.signum() < 0 || divider.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Divider must be a positive whole number");
        }
        BigDecimal quotient = value().divide(divider, currencyUnit().getDefaultFractionDigits(), RoundingMode.DOWN);
        BigDecimal remainder = value().subtract(quotient.multiply(divider));
        return new Money[] {Money.of(quotient, currency()), Money.of(remainder, currency())};
    }

    /**
     * Multiplies this amount by a Percentage without rounding.
     *
     * @param percentage percentage multiplier
     * @return product in the same currency
     */
    public Money multiply(Percentage percentage) {
        BigDecimal multiplier = percentage.value().divide(new BigDecimal(100), 30, HALF_UP);
        return multiply(multiplier);
    }

    /** Returns whether this amount is zero. */
    public boolean isZero() {
        return value.isZero();
    }

    /** Returns whether this amount is negative. */
    public boolean isNegative() {
        return value.isNegative();
    }

    /**
     * Returns whether this amount is greater than another same-currency amount.
     *
     * @param other amount to compare
     * @return true when this amount is greater
     * @throws MonetaryException when currencies differ
     */
    public boolean isGreaterThan(Money other) {
        return value.isGreaterThan(other.value);
    }

    /**
     * Returns whether this amount is at least another same-currency amount.
     *
     * @param other amount to compare
     * @return true when this amount is greater or equal
     * @throws MonetaryException when currencies differ
     */
    public boolean isGreaterThanOrEqualTo(Money other) {
        return value.isGreaterThanOrEqualTo(other.value);
    }

    /**
     * Returns the underlying decimal amount without application-level rounding or normalization.
     *
     * @return raw decimal amount
     */
    public BigDecimal value() {
        return value.getNumber().numberValue(BigDecimal.class);
    }

    /**
     * Returns the monetary value normalized to at most 10 fractional digits.
     *
     * <p>Rounds using the supplied mode, strips trailing zeros, and prevents scientific notation.
     *
     * @param roundingMode rounding mode used at the normalization boundary
     * @return the normalized monetary value
     */
    public BigDecimal value(RoundingMode roundingMode) {
        BigDecimal stripped = value().setScale(10, roundingMode).stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0, roundingMode) : stripped;
    }

    /** Returns the ISO 4217 currency code. */
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

    /** Returns one smallest unit of this amount's currency. */
    public Money smallestUnit() {
        return Money.of(BigDecimal.ONE.movePointLeft(currencyUnit().getDefaultFractionDigits()), currency());
    }

    /** Returns this amount's JSR 354 currency unit. */
    public CurrencyUnit currencyUnit() {
        return value.getCurrency();
    }
}

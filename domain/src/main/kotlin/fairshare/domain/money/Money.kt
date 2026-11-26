package fairshare.domain.money

import fairshare.domain.validation.InvalidInputException
import fairshare.domain.validation.ValidationErrorCode
import java.math.BigDecimal
import org.joda.money.Money as JodaMoney

/** An immutable signed amount in one currency, expressed in its standard minor units. */
@JvmInline
public value class Money private constructor(private val value: JodaMoney) {
    /** The exact amount at the currency's standard scale, not the original entered scale. */
    public val amount: BigDecimal get() = jodaValue(value.amount)

    /** The currency of this amount. */
    public val currency: Currency get() = Currency(jodaValue(value.currencyUnit))

    /** Whether this amount is strictly positive. */
    public val isPositive: Boolean get() = value.isPositive

    /** Whether this amount is strictly negative. */
    public val isNegative: Boolean get() = value.isNegative

    /** Whether this amount is zero. */
    public val isZero: Boolean get() = value.isZero

    /**
     * Adds [other] exactly.
     *
     * @throws IllegalArgumentException if the currencies differ.
     */
    public operator fun plus(other: Money): Money = Money(jodaValue(value.plus(other.value)))

    /**
     * Subtracts [other] exactly.
     *
     * @throws IllegalArgumentException if the currencies differ.
     */
    public operator fun minus(other: Money): Money = Money(jodaValue(value.minus(other.value)))

    /** Returns the negated amount in the same currency. */
    public operator fun unaryMinus(): Money = Money(jodaValue(value.negated()))

    public companion object {
        /**
         * Creates an amount without rounding. Zero and negative amounts are allowed.
         * Trailing zeros count toward the input scale.
         *
         * @throws InvalidInputException if the input scale exceeds the currency's standard scale.
         */
        public fun of(currency: Currency, amount: BigDecimal): Money = try {
            Money(jodaValue(JodaMoney.of(currency.unit, amount)))
        } catch (cause: ArithmeticException) {
            val scale = currency.unit.decimalPlaces
            if (amount.scale() <= scale) throw cause

            throw InvalidInputException(
                ValidationErrorCode.EXCESS_PRECISION,
                "Amount scale ${amount.scale()} exceeds the scale $scale for ${currency.code}",
                cause,
            )
        }
    }
}

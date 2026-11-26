package fairshare.domain.money

import fairshare.domain.validation.InvalidInputException
import fairshare.domain.validation.ValidationErrorCode
import java.math.BigDecimal
import java.math.RoundingMode
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

    /**
     * Multiplies by [multiplier] without changing currency. The result uses the currency's standard scale and
     * rounds using [roundingMode] only if needed. By default, rounding is not allowed; redundant zeros may be dropped.
     *
     * @throws ArithmeticException if rounding is needed but [roundingMode] is [RoundingMode.UNNECESSARY].
     */
    public fun multiply(multiplier: BigDecimal, roundingMode: RoundingMode = RoundingMode.UNNECESSARY): Money =
        Money(jodaValue(value.multipliedBy(multiplier, roundingMode)))

    /**
     * Divides by [divisor] without changing currency. The result uses the currency's standard scale and
     * rounds using [roundingMode] only if needed. By default, rounding is not allowed.
     *
     * @throws ArithmeticException if [divisor] is zero or rounding is needed but [roundingMode] is
     * [RoundingMode.UNNECESSARY].
     */
    public fun divide(divisor: BigDecimal, roundingMode: RoundingMode = RoundingMode.UNNECESSARY): Money =
        Money(jodaValue(value.dividedBy(divisor, roundingMode)))

    /**
     * Divides by a positive count and returns the quotient and remainder, in that order.
     *
     * **Implementation requirements:** Both amounts retain this currency and its standard scale. The quotient
     * rounds toward negative infinity, including for negative amounts. The remainder is exactly this amount minus
     * the quotient times [divisor].
     *
     * @throws IllegalArgumentException if [divisor] is not strictly positive.
     */
    public fun divideAndRemainder(divisor: Int): Pair<Money, Money> {
        require(divisor > 0) { "Divisor must be strictly positive" }
        val count = BigDecimal(divisor)
        val quotient = divide(count, RoundingMode.FLOOR)
        val remainder = this - quotient.multiply(count)
        return quotient to remainder
    }

    public companion object {
        /**
         * Creates an amount without rounding. Zero and negative amounts are allowed.
         * Trailing zeros count toward the input scale.
         *
         * @throws InvalidInputException if the input scale exceeds the currency's standard scale.
         */
        public fun of(currency: Currency, amount: BigDecimal): Money {
            val scale = currency.unit.decimalPlaces

            if (amount.scale() > scale) {
                throw InvalidInputException(
                    ValidationErrorCode.EXCESS_PRECISION,
                    "Amount scale ${amount.scale()} exceeds the scale $scale for ${currency.code}",
                )
            }

            return Money(jodaValue(JodaMoney.of(currency.unit, amount)))
        }
    }
}

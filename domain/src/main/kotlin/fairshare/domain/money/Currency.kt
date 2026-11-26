package fairshare.domain.money

import fairshare.domain.validation.InvalidInputException
import fairshare.domain.validation.ValidationErrorCode
import org.joda.money.CurrencyUnit
import org.joda.money.IllegalCurrencyException

/** An ISO 4217 currency with defined standard minor units, usable as a Group's Settlement currency. */
@JvmInline
public value class Currency internal constructor(internal val unit: CurrencyUnit) {
    init {
        if (unit.isPseudoCurrency) {
            throw InvalidInputException(
                ValidationErrorCode.UNSUPPORTED_CURRENCY,
                "Currency ${unit.code} has no defined minor units",
            )
        }
    }

    /** The exact uppercase ISO 4217 code. */
    public val code: String get() = jodaValue(unit.code)

    public companion object {
        /**
         * Resolves an exact uppercase ISO 4217 code without normalization.
         * Uses Joda-Money's standard currency data; registry extensions and overrides are unsupported.
         *
         * @throws InvalidInputException if the code is unsupported or has no defined minor units.
         */
        public fun of(code: String): Currency = try {
            Currency(jodaValue(CurrencyUnit.of(code)))
        } catch (cause: IllegalCurrencyException) {
            throw InvalidInputException(ValidationErrorCode.UNSUPPORTED_CURRENCY, "Unsupported currency '$code'", cause)
        }
    }
}

package fairshare.domain.group

import java.util.Currency

@JvmInline
public value class SettlementCurrency private constructor(public val code: String) {
    public val minorUnits: Int get() = Currency.getInstance(code).defaultFractionDigits

    public companion object {
        public fun parse(code: String): SettlementCurrency? {
            val currency = try {
                Currency.getInstance(code)
            } catch (_: IllegalArgumentException) {
                return null
            }
            return if (currency.defaultFractionDigits >= 0) SettlementCurrency(code) else null
        }
    }
}

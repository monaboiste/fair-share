package fairshare.domain.group

import java.util.Currency

@JvmInline
public value class GroupName private constructor(public val value: String) {
    public companion object {
        public fun parse(value: String): GroupName? = if (value.isBlank()) null else GroupName(value)
    }
}

@JvmInline
public value class SettlementCurrency private constructor(public val code: String) {
    public companion object {
        public fun parse(code: String): SettlementCurrency? {
            val currency = try {
                Currency.getInstance(code)
            } catch (_: IllegalArgumentException) {
                return null
            }
            return SettlementCurrency(code).takeIf { currency.defaultFractionDigits >= 0 }
        }
    }
}

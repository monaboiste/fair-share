package fairshare.domain.group

import fairshare.domain.group.exception.InvalidSettlementCurrencyException
import org.joda.money.CurrencyUnit
import org.joda.money.IllegalCurrencyException

internal object SettlementCurrencyRule {
    fun minorUnits(code: String): Int {
        val currency = try {
            CurrencyUnit.of(code)
        } catch (cause: IllegalCurrencyException) {
            throw InvalidSettlementCurrencyException(cause)
        }
        if (currency.isPseudoCurrency) throw InvalidSettlementCurrencyException()
        return currency.decimalPlaces
    }
}

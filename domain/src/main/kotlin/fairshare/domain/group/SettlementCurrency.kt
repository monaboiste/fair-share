package fairshare.domain.group

public data class SettlementCurrency(public val code: String) {
    public val minorUnits: Int = SettlementCurrencyRule.minorUnits(code)
}

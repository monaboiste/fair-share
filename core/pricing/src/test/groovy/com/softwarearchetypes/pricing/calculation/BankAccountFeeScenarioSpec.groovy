package com.softwarearchetypes.pricing.calculation

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class BankAccountFeeScenarioSpec extends Specification {
    def "income tiers select the expected fee"() {
        given:
        def low = Calculators.fixed("low", Money.of(new BigDecimal("20"), "PLN"))
        def high = Calculators.fixed("high", Money.of(new BigDecimal("10"), "PLN"))
        def calculator = Calculators.composite("fees", "monthlyIncome", [
            new NumericRange(BigDecimal.ZERO, new BigDecimal("1000"), low.id),
            new NumericRange(new BigDecimal("1000"), new BigDecimal("4000"), high.id)
        ], [low, high])

        expect:
        calculator.calculate(Parameters.of("monthlyIncome", income)).money() == Money.of(amount, "PLN")

        where:
        income | amount
        0      | new BigDecimal("20")
        1000   | new BigDecimal("10")
    }
}

package com.softwarearchetypes.pricing.calculation

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class CompositeCalculatorSpec extends Specification {
    def "composite selects ranges at their boundaries"() {
        given:
        def first = Calculators.fixed("first", Money.of(20, "PLN"))
        def second = Calculators.fixed("second", Money.of(10, "PLN"))
        def calculator = Calculators.composite("fees", "quantity", [
            new NumericRange(BigDecimal.ZERO, BigDecimal.TEN, first.id),
            new NumericRange(BigDecimal.TEN, new BigDecimal("20"), second.id)
        ], [first, second])

        expect:
        calculator.calculate(Parameters.of("quantity", quantity)).money() == Money.of(expected, "PLN")

        where:
        quantity | expected
        0        | 20
        10       | 10
    }
}

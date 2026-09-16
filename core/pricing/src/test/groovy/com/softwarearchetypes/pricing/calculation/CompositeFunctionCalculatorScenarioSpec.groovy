package com.softwarearchetypes.pricing.calculation

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class CompositeFunctionCalculatorScenarioSpec extends Specification {
    def "piecewise calculator calculates selected shipping fee"() {
        given:
        def light = Calculators.fixed("light", Money.of(5, "PLN"))
        def heavy = Calculators.fixed("heavy", Money.of(15, "PLN"))
        def calculator = Calculators.composite("shipping", "weight", [
            new NumericRange(BigDecimal.ZERO, BigDecimal.TEN, light.id),
            new NumericRange(BigDecimal.TEN, new BigDecimal("100"), heavy.id)
        ], [light, heavy])

        expect:
        calculator.calculate(Parameters.of("weight", weight)).money() == Money.of(expected, "PLN")

        where:
        weight | expected
        3      | 5
        10      | 15
    }
}

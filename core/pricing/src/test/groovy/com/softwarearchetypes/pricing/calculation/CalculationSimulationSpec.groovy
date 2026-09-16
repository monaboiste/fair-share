package com.softwarearchetypes.pricing.calculation

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class CalculationSimulationSpec extends Specification {
    def "simulation preserves point order and results"() {
        given:
        def calculator = Calculators.fixed("fee", Money.of(new BigDecimal("10"), "PLN"))
        def points = [Parameters.of("quantity", 1), Parameters.of("quantity", 2)]

        expect:
        calculator.simulate(points).keySet().toList() == points
        calculator.simulate(points).values()*.money() == [Money.of(new BigDecimal("10"), "PLN")] * 2
    }
}

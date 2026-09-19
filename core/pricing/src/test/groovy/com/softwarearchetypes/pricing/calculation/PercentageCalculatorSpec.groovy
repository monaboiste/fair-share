package com.softwarearchetypes.pricing.calculation

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class PercentageCalculatorSpec extends Specification {

    def "calculates and rounds a percentage of the base amount"() {
        given:
        def calculator = new PercentageCalculator("tax", new BigDecimal("23"))
        def parameters = Parameters.of("baseAmount", Money.of(new BigDecimal("10.01"), "PLN"))

        when:
        def result = calculator.calculate(parameters)

        then:
        result.money() == Money.of(new BigDecimal("2.30"), "PLN")
    }

    def "describes its contract and exposes identity"() {
        given:
        def calculator = new PercentageCalculator("tax", new BigDecimal("23"))

        expect:
        calculator.name() == "tax"
        calculator.describe().contains("23")
        calculator.formula() == "baseAmount × 23%"
        calculator.id() != null
    }

    def "requires a base amount"() {
        given:
        def calculator = new PercentageCalculator("tax", BigDecimal.TEN)

        when:
        calculator.calculate(Parameters.empty())

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("baseAmount")
    }
}

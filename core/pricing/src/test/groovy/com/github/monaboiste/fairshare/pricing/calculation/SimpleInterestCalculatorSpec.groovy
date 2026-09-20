package com.github.monaboiste.fairshare.pricing.calculation

import com.github.monaboiste.fairshare.quantity.money.Money
import java.math.RoundingMode
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class SimpleInterestCalculatorSpec extends Specification {

    def "calculates annual interest for supported time units"() {
        given:
        def calculator = new SimpleInterestCalculator("interest", new BigDecimal("36.5"))
        def parameters = Parameters.of("base", Money.of(365, "PLN"), "unit", unit)

        when:
        def result = calculator.calculate(parameters)

        then:
        result.money().value().setScale(2, RoundingMode.HALF_UP) == expected

        where:
        unit             | expected
        ChronoUnit.DAYS   | new BigDecimal("0.37")
        ChronoUnit.WEEKS  | new BigDecimal("2.56")
        ChronoUnit.MONTHS | new BigDecimal("11.10")
        ChronoUnit.YEARS  | new BigDecimal("133.23")
    }

    def "rejects unsupported time units"() {
        given:
        def calculator = new SimpleInterestCalculator("interest", BigDecimal.TEN)
        def parameters = Parameters.of("base", Money.of(100, "PLN"), "unit", ChronoUnit.HOURS)

        when:
        calculator.calculate(parameters)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains("Unsupported unit")
    }

    def "describes its contract and exposes identity"() {
        given:
        def calculator = new SimpleInterestCalculator("interest", new BigDecimal("12.5"))

        expect:
        calculator.describe().contains("12.5")
        calculator.formula().contains("12.5")
        calculator.id() != null
    }
}

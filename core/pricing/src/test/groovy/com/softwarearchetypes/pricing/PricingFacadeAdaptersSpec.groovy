package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class PricingFacadeAdaptersSpec extends Specification {

    private final PricingFacade facade = PricingTestConfiguration.inMemory(Clock.systemUTC())

    def "calculateTotal returns directly when the calculator already returns total"() {
        given:
        facade.addCalculator(
                "total-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(150, "PLN"))
        )
        Money total = facade.calculateTotal("total-calc", Parameters.empty())

        expect:
        total == Money.of(150, "PLN")
    }

    def "calculateTotal wraps a unit-price calculator"() {
        given:
        facade.addCalculator(
                "unit-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(10, "PLN"),
                        "interpretation", Interpretation.UNIT
                )
        )
        Money total = facade.calculateTotal("unit-calc", Parameters.of("quantity", new BigDecimal("15")))

        expect:
        total == Money.of(150, "PLN")
    }

    def "calculateTotal wraps a marginal-price calculator"() {
        given:
        facade.addCalculator(
                "marginal-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(10, "PLN"),
                        "interpretation", Interpretation.MARGINAL
                )
        )
        Money total = facade.calculateTotal("marginal-calc", Parameters.of("quantity", new BigDecimal("5")))

        expect:
        total == Money.of(50, "PLN")
    }

    def "calculateUnitPrice returns directly when the calculator already returns a unit price"() {
        given:
        facade.addCalculator(
                "unit-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(10, "PLN"),
                        "interpretation", Interpretation.UNIT
                )
        )
        Money unit = facade.calculateUnitPrice("unit-calc", Parameters.empty())

        expect:
        unit == Money.of(10, "PLN")
    }

    def "calculateUnitPrice wraps a total-price calculator"() {
        given:
        facade.addCalculator(
                "step-calc",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(100, "PLN"),
                        "stepSize", new BigDecimal("10"),
                        "stepIncrement", new BigDecimal("5")
                )
        )
        Money unit = facade.calculateUnitPrice("step-calc", Parameters.of("quantity", new BigDecimal("15")))

        expect:
        unit == Money.of(7, "PLN")
    }

    def "calculateUnitPrice wraps a marginal-price calculator"() {
        given:
        facade.addCalculator(
                "marginal-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(10, "PLN"),
                        "interpretation", Interpretation.MARGINAL
                )
        )
        Money unit = facade.calculateUnitPrice("marginal-calc", Parameters.of("quantity", new BigDecimal("5")))

        expect:
        unit == Money.of(10, "PLN")
    }

    def "calculateMarginal returns directly when the calculator already returns marginal"() {
        given:
        facade.addCalculator(
                "marginal-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(10, "PLN"),
                        "interpretation", Interpretation.MARGINAL
                )
        )
        Money marginal = facade.calculateMarginal("marginal-calc", Parameters.empty())

        expect:
        marginal == Money.of(10, "PLN")
    }

    def "calculateMarginal wraps a unit-price calculator"() {
        given:
        facade.addCalculator(
                "unit-calc",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(10, "PLN"),
                        "interpretation", Interpretation.UNIT
                )
        )
        Money marginal = facade.calculateMarginal("unit-calc", Parameters.of("quantity", new BigDecimal("5")))

        expect:
        marginal == Money.of(10, "PLN")
    }

    def "calculateMarginal wraps a total-price calculator"() {
        given:
        facade.addCalculator(
                "step-calc",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(100, "PLN"),
                        "stepSize", new BigDecimal("1"),
                        "stepIncrement", new BigDecimal("5")
                )
        )
        Money marginal = facade.calculateMarginal("step-calc", Parameters.of("quantity", new BigDecimal("11")))

        expect:
        marginal == Money.of(5, "PLN")
    }

    def "facade rejects direct creation of adapter calculator types"() {
        when:
        facade.addCalculator(
                "adapter",
                CalculatorType.UNIT_TO_TOTAL_ADAPTER,
                Parameters.empty()
        )

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("cannot be created directly")
    }

    def "auto-wrapping all three interpretations in a complex scenario"() {
        given:
        facade.addCalculator(
                "bulk",
                CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(100, "PLN"),
                        "stepSize", new BigDecimal("10"),
                        "stepIncrement", new BigDecimal("5")
                )
        )

        Parameters params = Parameters.of("quantity", new BigDecimal("25"))
        Money total = facade.calculateTotal("bulk", params)

        expect:
        total == Money.of(110, "PLN")
        Money unit = facade.calculateUnitPrice("bulk", params)
        unit == Money.of(new BigDecimal("4.40"), "PLN")
        Money marginal = facade.calculateMarginal("bulk", params)
        marginal == Money.of(0, "PLN")
    }
}

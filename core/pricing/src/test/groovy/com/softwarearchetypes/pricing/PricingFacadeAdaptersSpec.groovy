package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class PricingFacadeAdaptersSpec extends Specification {

    private final PricingFacade facade = PricingTestConfiguration.inMemory(Clock.systemUTC())

    def "calculateTotal returns directly when the calculator already returns total"() {
        given:
        facade.addCalculator(Calculators.fixed("total-calc", Money.of(150, "PLN")))
        Money total = facade.calculateTotal("total-calc", Parameters.empty())

        expect:
        total == Money.of(150, "PLN")
    }

    def "calculateTotal wraps a unit-price calculator"() {
        given:
        facade.addCalculator(Calculators.fixed("unit-calc", Money.of(10, "PLN"), Interpretation.UNIT))
        Money total = facade.calculateTotal("unit-calc", Parameters.of("quantity", new BigDecimal("15")))

        expect:
        total == Money.of(150, "PLN")
    }

    def "calculateTotal wraps a marginal-price calculator"() {
        given:
        facade.addCalculator(Calculators.fixed("marginal-calc", Money.of(10, "PLN"), Interpretation.MARGINAL))
        Money total = facade.calculateTotal("marginal-calc", Parameters.of("quantity", new BigDecimal("5")))

        expect:
        total == Money.of(50, "PLN")
    }

    def "calculateUnitPrice returns directly when the calculator already returns a unit price"() {
        given:
        facade.addCalculator(Calculators.fixed("unit-calc", Money.of(10, "PLN"), Interpretation.UNIT))
        Money unit = facade.calculateUnitPrice("unit-calc", Parameters.empty())

        expect:
        unit == Money.of(10, "PLN")
    }

    def "calculateUnitPrice wraps a total-price calculator"() {
        given:
        facade.addCalculator(Calculators.stepFunction("step-calc", Money.of(100, "PLN"), new BigDecimal("10"), new BigDecimal("5")))
        Money unit = facade.calculateUnitPrice("step-calc", Parameters.of("quantity", new BigDecimal("15")))

        expect:
        unit == Money.of(7, "PLN")
    }

    def "calculateUnitPrice wraps a marginal-price calculator"() {
        given:
        facade.addCalculator(Calculators.fixed("marginal-calc", Money.of(10, "PLN"), Interpretation.MARGINAL))
        Money unit = facade.calculateUnitPrice("marginal-calc", Parameters.of("quantity", new BigDecimal("5")))

        expect:
        unit == Money.of(10, "PLN")
    }

    def "calculateMarginal returns directly when the calculator already returns marginal"() {
        given:
        facade.addCalculator(Calculators.fixed("marginal-calc", Money.of(10, "PLN"), Interpretation.MARGINAL))
        Money marginal = facade.calculateMarginal("marginal-calc", Parameters.empty())

        expect:
        marginal == Money.of(10, "PLN")
    }

    def "calculateMarginal wraps a unit-price calculator"() {
        given:
        facade.addCalculator(Calculators.fixed("unit-calc", Money.of(10, "PLN"), Interpretation.UNIT))
        Money marginal = facade.calculateMarginal("unit-calc", Parameters.of("quantity", new BigDecimal("5")))

        expect:
        marginal == Money.of(10, "PLN")
    }

    def "calculateMarginal wraps a total-price calculator"() {
        given:
        facade.addCalculator(Calculators.stepFunction("step-calc", Money.of(100, "PLN"), new BigDecimal("1"), new BigDecimal("5")))
        Money marginal = facade.calculateMarginal("step-calc", Parameters.of("quantity", new BigDecimal("11")))

        expect:
        marginal == Money.of(5, "PLN")
    }

}

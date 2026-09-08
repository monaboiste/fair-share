package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class AdaptersSpec extends Specification {

    def "unit-to-total adapter multiplies by quantity"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator totalCalculator = UnitToTotalAdapter.wrap("adapter", unitCalculator)
        Money total = totalCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        total == Money.of(150, "PLN")
        totalCalculator.interpretation() == Interpretation.TOTAL
    }

    def "unit-to-total adapter rejects a non-unit-price calculator"() {
        given:
        Calculator totalCalculator = new SimpleFixedCalculator(
                "total",
                Money.of(100, "PLN"),
                Interpretation.TOTAL
        )

        when:
        UnitToTotalAdapter.wrap("adapter", totalCalculator)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("UNIT")
    }

    def "unit-to-marginal adapter returns the same price regardless of quantity"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator marginalCalculator = UnitToMarginalAdapter.wrap("adapter", unitCalculator)
        Money marginal5 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))
        Money marginal15 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        marginal5 == Money.of(10, "PLN")
        marginal15 == Money.of(10, "PLN")
        marginalCalculator.interpretation() == Interpretation.MARGINAL
    }

    def "total-to-unit adapter divides total by quantity"() {
        given:
        Calculator totalCalculator = new StepFunctionCalculator(
                "bulk-pricing",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.TOTAL
        )
        Calculator unitCalculator = TotalToUnitAdapter.wrap("adapter", totalCalculator)
        Money unit = unitCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        unit == Money.of(7, "PLN")
        unitCalculator.interpretation() == Interpretation.UNIT
    }

    def "total-to-marginal adapter computes the marginal as the derivative"() {
        given:
        Calculator totalCalculator = new StepFunctionCalculator(
                "step-pricing",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.TOTAL
        )
        Calculator marginalCalculator = TotalToMarginalAdapter.wrap("adapter", totalCalculator)
        Money marginal1 = marginalCalculator.calculate(Parameters.of("quantity", BigDecimal.ONE))

        expect:
        marginal1 == Money.of(100, "PLN")
        Money marginal10 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("10")))
        marginal10 == Money.of(5, "PLN")
        Money marginal11 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("11")))
        marginal11 == Money.of(0, "PLN")

        marginalCalculator.interpretation() == Interpretation.MARGINAL
    }

    def "marginal-to-total adapter sums marginal prices"() {
        given:
        Calculator marginalCalculator = new SimpleFixedCalculator(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator totalCalculator = MarginalToTotalAdapter.wrap("adapter", marginalCalculator)
        Money total = totalCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))

        expect:
        total == Money.of(50, "PLN")
        totalCalculator.interpretation() == Interpretation.TOTAL
    }

    def "marginal-to-unit adapter returns the average price"() {
        given:
        Calculator marginalCalculator = new SimpleFixedCalculator(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator unitCalculator = MarginalToUnitAdapter.wrap("adapter", marginalCalculator)
        Money unit = unitCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))

        expect:
        unit == Money.of(10, "PLN")
        unitCalculator.interpretation() == Interpretation.UNIT
    }

    def "unit-to-marginal adapter works for a variable unit price"() {
        given:
        Calculator unitCalculator = new StepFunctionCalculator(
                "bulk-unit-price",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.UNIT
        )
        Calculator marginalCalculator = UnitToMarginalAdapter.wrap("adapter", unitCalculator)
        Money marginal6 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("6")))
        Money marginal11 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("11")))

        expect:
        marginal6 != null
        marginal11 != null
        marginalCalculator.interpretation() == Interpretation.MARGINAL
    }

    def "each adapter reports its correct calculator type"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator("u", Money.of(10, "PLN"), Interpretation.UNIT)
        Calculator totalCalculator = new SimpleFixedCalculator("t", Money.of(100, "PLN"), Interpretation.TOTAL)
        Calculator marginalCalculator = new SimpleFixedCalculator("m", Money.of(10, "PLN"), Interpretation.MARGINAL)

        expect:
        UnitToTotalAdapter.wrap("a", unitCalculator).getType() == CalculatorType.UNIT_TO_TOTAL_ADAPTER
        UnitToMarginalAdapter.wrap("a", unitCalculator).getType() == CalculatorType.UNIT_TO_MARGINAL_ADAPTER
        TotalToUnitAdapter.wrap("a", totalCalculator).getType() == CalculatorType.TOTAL_TO_UNIT_ADAPTER
        TotalToMarginalAdapter.wrap("a", totalCalculator).getType() == CalculatorType.TOTAL_TO_MARGINAL_ADAPTER
        MarginalToTotalAdapter.wrap("a", marginalCalculator).getType() == CalculatorType.MARGINAL_TO_TOTAL_ADAPTER
        MarginalToUnitAdapter.wrap("a", marginalCalculator).getType() == CalculatorType.MARGINAL_TO_UNIT_ADAPTER
    }

    def "adapter formula includes the source calculator formula"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator("test", Money.of(10, "PLN"), Interpretation.UNIT)

        Calculator totalAdapter = UnitToTotalAdapter.wrap("adapter", unitCalculator)

        String formula = totalAdapter.formula()

        expect:
        formula.contains("quantity \u00d7")
        formula.contains("f(x) = PLN 10")
    }
}

package com.softwarearchetypes.pricing.calculation

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class CompositeCalculatorSpec extends Specification {
    Calculator fixed = Calculators.fixed("fixed-100", Money.of(100, "PLN"))
    Calculator step = Calculators.stepFunction("step-calc", Money.of(200, "PLN"), new BigDecimal("10"), new BigDecimal("10"), Interpretation.TOTAL, StepBoundary.EXCLUSIVE)
    Calculator discrete = Calculators.discretePoints("discrete-calc", Map.of(new BigDecimal("50"), Money.of(500, "PLN"), new BigDecimal("75"), Money.of(700, "PLN")), Interpretation.TOTAL)

    private Calculator composite(String name, CalculatorRange... ranges) {
        new CompositeFunctionCalculator(name, new Ranges("quantity", ranges.toList()), [fixed, step, discrete])
    }

    def "delegates to each matching range calculator"() {
        expect:
        composite("composite", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id), CalculatorRange.numeric(50, 100, discrete.id)).calculate(Parameters.of("quantity", value)).money() == Money.of(amount, "PLN")
        where:
        value | amount
        5     | 100
        15    | 210
        75    | 700
    }

    def "handles range boundaries correctly"() {
        given:
        Calculator calculator = composite("piecewise-pricing", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id))

        expect:
        calculator.calculate(Parameters.of("quantity", 10G)).money().value() == 210G
        calculator.calculate(Parameters.of("quantity", 9G)).money().value() == 100G
    }

    def "throws when value outside all ranges"() {
        when:
        composite("piecewise-pricing", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id)).calculate(Parameters.of("quantity", 100G))
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("No matching range")
    }

    def "throws when referenced calculator not found"() {
        when:
        new CompositeFunctionCalculator("piecewise-pricing", Ranges.of("quantity", CalculatorRange.numeric(0, 10, CalculatorId.generate())), [fixed])
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("not found")
    }

    def "throws when parameter missing"() {
        when:
        composite("piecewise-pricing", CalculatorRange.numeric(0, 10, fixed.id)).calculate(Parameters.empty())
        then:
        thrown(IllegalArgumentException)
    }

    def "provides description"() {
        expect:
        composite("piecewise-pricing", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id)).describe().contains("Composite function calculator")
        composite("piecewise-pricing", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id)).describe().contains("quantity")
    }

    def "fails during construction when calculator not found"() {
        when:
        new CompositeFunctionCalculator("composite", Ranges.of("quantity", CalculatorRange.numeric(0, 10, CalculatorId.generate())), [fixed])
        then:
        thrown(IllegalArgumentException)
    }

    def "returns total behavior for total component calculators"() {
        expect:
        composite("composite", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id)).calculate(Parameters.of("quantity", 15G)).money() == Money.of(210, "PLN")
    }

    def "formula describes each selected tier"() {
        expect:
        composite("composite", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id)).formula().contains("[0, 10)")
        composite("composite", CalculatorRange.numeric(0, 10, fixed.id), CalculatorRange.numeric(10, 50, step.id)).formula().contains("fixed-100")
    }

    def "rejects missing parameter before range selection"() {
        when:
        composite("composite", CalculatorRange.numeric(0, 10, fixed.id)).calculate(Parameters.empty())
        then:
        thrown(IllegalArgumentException)
    }
}

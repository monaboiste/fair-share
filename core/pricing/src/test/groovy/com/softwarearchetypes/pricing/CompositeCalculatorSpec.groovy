package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class CompositeCalculatorSpec extends Specification {

    private PricingFacade facade
    private CalculatorId fixedId
    private CalculatorId stepId
    private CalculatorId discreteId
    private Map<CalculatorId, Calculator> calculators

    def setup() {
        facade = PricingTestConfiguration.inMemory(Clock.systemUTC())
        calculators = [:]
        fixedId = addFixedCalculator("fixed-100", Money.of(100, "PLN"))
        Calculator step = facade.addCalculator(Calculators.stepFunction("step-calc", Money.of(200, "PLN"), new BigDecimal("10"), new BigDecimal("10"), Interpretation.TOTAL, StepBoundary.EXCLUSIVE))
        calculators[step.getId()] = step
        stepId = step.getId()
        Calculator discrete = facade.addCalculator(Calculators.discretePoints("discrete-calc", Map.of(
                                new BigDecimal("50"), Money.of(500, "PLN"),
                                new BigDecimal("75"), Money.of(700, "PLN")), Interpretation.TOTAL))
        calculators[discrete.getId()] = discrete
        discreteId = discrete.getId()
    }

    def "delegates to first range calculator"() {
        given:
        addCompositeCalculator(
                "composite",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
                CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), discreteId))

        when:
        Money result = facade.calculate("composite", Parameters.of("quantity", new BigDecimal("5")))

        then:
        new BigDecimal("100.00") == result.value()
    }

    def "delegates to second range calculator"() {
        given:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
                CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), discreteId))

        when:
        Money result = facade.calculate("piecewise-pricing", Parameters.of("quantity", new BigDecimal("15")))

        then:
        new BigDecimal("210.00") == result.value()
    }

    def "delegates to third range calculator"() {
        given:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
                CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), discreteId))

        when:
        Money result = facade.calculate("piecewise-pricing", Parameters.of("quantity", new BigDecimal("75")))

        then:
        new BigDecimal("700.00") == result.value()
    }

    def "handles range boundaries correctly"() {
        given:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId))

        when:
        Money resultAt10 = facade.calculate("piecewise-pricing", Parameters.of("quantity", new BigDecimal("10")))
        Money resultAt9 = facade.calculate("piecewise-pricing", Parameters.of("quantity", new BigDecimal("9")))

        then:
        new BigDecimal("210.00") == resultAt10.value()
        new BigDecimal("100.00") == resultAt9.value()
    }

    def "throws when value outside all ranges"() {
        given:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId))

        when:
        facade.calculate("piecewise-pricing", Parameters.of("quantity", new BigDecimal("100")))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("No matching range")
    }

    def "throws when referenced calculator not found"() {
        given:
        CalculatorId nonExistentId = CalculatorId.generate()

        when:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), nonExistentId))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("not found")
    }

    def "throws when parameter missing"() {
        given:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId))

        when:
        facade.calculate("piecewise-pricing", Parameters.empty())

        then:
        thrown(IllegalArgumentException)
    }



    def "fails during construction when calculator not found"() {
        given:
        CalculatorId nonExistentId = CalculatorId.generate()

        when:
        addCompositeCalculator(
                "composite",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), nonExistentId))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("not found")
    }




    private CalculatorId addFixedCalculator(
            String name, Money amount, Interpretation interpretation = Interpretation.TOTAL) {
        Calculator calculator = facade.addCalculator(Calculators.fixed(name, amount, interpretation))
        calculators[calculator.getId()] = calculator
        calculator.getId()
    }

    private Calculator addCompositeCalculator(String name, CalculatorRange... ranges) {
        facade.addCalculator(Calculators.composite(name, "quantity", ranges.toList(), calculators.values()))
    }
}

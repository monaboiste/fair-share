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

    def "returns correct type"() {
        when:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId))

        then:
        facade.availableCalculators().find { it.name() == "piecewise-pricing" }.type() == CalculatorType.COMPOSITE
    }

    def "provides description"() {
        when:
        addCompositeCalculator(
                "piecewise-pricing",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId))

        then:
        String description = facade.availableCalculators().find { it.name() == "piecewise-pricing" }.description()
        description.contains("Composite function calculator")
        description.contains("quantity")
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

    def "fails when component calculators have different interpretations"() {
        given:
        CalculatorId totalId = addFixedCalculator("total-calc", Money.of(100, "PLN"), Interpretation.TOTAL)
        CalculatorId unitId = addFixedCalculator("unit-calc", Money.of(10, "PLN"), Interpretation.UNIT)

        when:
        addCompositeCalculator(
                "composite",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), totalId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), unitId))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("same interpretation")
        ex.message.contains("TOTAL")
        ex.message.contains("UNIT")
    }

    def "returns shared interpretation of component calculators"() {
        given:
        CalculatorId unit1Id = addFixedCalculator("unit-1", Money.of(10, "PLN"), Interpretation.UNIT)
        CalculatorId unit2Id = addFixedCalculator("unit-2", Money.of(8, "PLN"), Interpretation.UNIT)

        when:
        Calculator calculator = addCompositeCalculator(
                "composite",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), unit1Id),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), unit2Id))

        then:
        calculator.interpretation() == Interpretation.UNIT
    }

    def "allows composite with all total price calculators"() {
        when:
        Calculator calculator = addCompositeCalculator(
                "composite",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId))

        then:
        calculator.interpretation() == Interpretation.TOTAL
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

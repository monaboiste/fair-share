package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.util.Map
import spock.lang.Specification

class CompositeCalculatorSpec extends Specification {

    private CalculatorRepository repository

    def setup() {
        repository = new InMemoryCalculatorsRepository()
        repository.save(new SimpleFixedCalculator("fixed-100", Money.of(100, "PLN")))

        repository.save(new StepFunctionCalculator(
            "step-calc",
            Money.of(200, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("10")
        ))

        repository.save(new DiscretePointsCalculator("discrete-calc", Map.of(
            new BigDecimal("50"), Money.of(500, "PLN"),
            new BigDecimal("75"), Money.of(700, "PLN")
        )))
    }

    def "delegates to first range calculator"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()
        CalculatorId discreteId = repository.findByName("discrete-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
            CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), discreteId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "composite", ranges, repository
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5")))

        and:
        Money result = calculator.calculate(params)

        expect:

        new BigDecimal("100.00").compareTo(result.value()) == 0
    }

    def "delegates to second range calculator"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()
        CalculatorId discreteId = repository.findByName("discrete-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
            CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), discreteId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("15")))

        and:
        Money result = calculator.calculate(params)

        expect:

        new BigDecimal("210.00").compareTo(result.value()) == 0
    }

    def "delegates to third range calculator"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()
        CalculatorId discreteId = repository.findByName("discrete-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
            CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), discreteId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("75")))

        and:
        Money result = calculator.calculate(params)

        expect:

        new BigDecimal("700.00").compareTo(result.value()) == 0
    }

    def "handles range boundaries correctly"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        and:
        Parameters params10 = new Parameters(Map.of("quantity", new BigDecimal("10")))
        Money result10 = calculator.calculate(params10)

        expect:

        new BigDecimal("210.00").compareTo(result10.value()) == 0

        and:
        Parameters params9 = new Parameters(Map.of("quantity", new BigDecimal("9")))
        Money result9 = calculator.calculate(params9)
        new BigDecimal("100.00").compareTo(result9.value()) == 0
    }

    def "throws when value outside all ranges"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("100")))

        when:
        calculator.calculate(params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("No matching range")
    }

    def "throws when referenced calculator not found"() {

        given:
        CalculatorId nonExistentId = CalculatorId.generate()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), nonExistentId)
        )

        when:
        new CompositeFunctionCalculator("piecewise-pricing", ranges, repository)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("not found")
    }

    def "throws when parameter missing"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        Parameters params = Parameters.empty()

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }

    def "returns correct type"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        expect:

        calculator.getType() == CalculatorType.COMPOSITE
    }

    def "provides description"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        and:
        String description = calculator.describe()

        expect:

        description.contains("Composite function calculator")
        description.contains("quantity")
    }

    def "fails during construction when calculator not found"() {

        given:
        CalculatorId nonExistentId = CalculatorId.generate()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), nonExistentId)
        )

        when:
        new CompositeFunctionCalculator("composite", ranges, repository)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("not found in repository")
    }

    def "fails when component calculators have different interpretations"() {

        given:
        repository.save(new SimpleFixedCalculator("total-calc", Money.of(100, "PLN"), Interpretation.TOTAL))
        repository.save(new SimpleFixedCalculator("unit-calc", Money.of(10, "PLN"), Interpretation.UNIT))

        CalculatorId totalId = repository.findByName("total-calc").get().getId()
        CalculatorId unitId = repository.findByName("unit-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), totalId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), unitId)
        )

        when:
        new CompositeFunctionCalculator("composite", ranges, repository)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("same interpretation")
        ex.message.contains("TOTAL")
        ex.message.contains("UNIT")
    }

    def "returns shared interpretation of component calculators"() {

        given:
        repository.save(new SimpleFixedCalculator("unit-1", Money.of(10, "PLN"), Interpretation.UNIT))
        repository.save(new SimpleFixedCalculator("unit-2", Money.of(8, "PLN"), Interpretation.UNIT))

        CalculatorId unit1Id = repository.findByName("unit-1").get().getId()
        CalculatorId unit2Id = repository.findByName("unit-2").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), unit1Id),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), unit2Id)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "composite", ranges, repository
        )

        expect:

        calculator.interpretation() == Interpretation.UNIT
    }

    def "allows composite with all total price calculators"() {

        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()
        CalculatorId stepId = repository.findByName("step-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId)
        )

        and:
        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "composite", ranges, repository
        )

        expect:

        calculator.interpretation() == Interpretation.TOTAL
        calculator != null
    }
}

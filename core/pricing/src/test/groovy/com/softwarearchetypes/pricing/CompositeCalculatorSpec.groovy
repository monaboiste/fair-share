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
    def "should delegate to first range calculator"() {        given:
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
        and:
        assert new BigDecimal("100.00").compareTo(result.value()) == 0
    }
    def "should delegate to second range calculator"() {        given:
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
        assert new BigDecimal("210.00").compareTo(result.value()) == 0
    }
    def "should delegate to third range calculator"() {        given:
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
        assert new BigDecimal("700.00").compareTo(result.value()) == 0
    }
    def "should handle range boundaries correctly"() {        given:
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
        assert new BigDecimal("210.00").compareTo(result10.value()) == 0
        and:
        Parameters params9 = new Parameters(Map.of("quantity", new BigDecimal("9")))
        Money result9 = calculator.calculate(params9)
        assert new BigDecimal("100.00").compareTo(result9.value()) == 0
    }
    def "should throw when value outside all ranges"() {        given:
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
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { calculator.calculate(params) }

        assert exception.getMessage().contains("No matching range")
    }
    def "should throw when referenced calculator not found"() {        given:
        CalculatorId nonExistentId = CalculatorId.generate()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), nonExistentId)
        )
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { new CompositeFunctionCalculator("piecewise-pricing", ranges, repository) }

        assert exception.getMessage().contains("not found")
    }
    def "should throw when parameter missing"() {        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )

        Parameters params = Parameters.empty()
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "should return correct type"() {        given:
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId)
        )

        CompositeFunctionCalculator calculator = new CompositeFunctionCalculator(
            "piecewise-pricing", ranges, repository
        )
        and:
        assert calculator.getType() == CalculatorType.COMPOSITE
    }
    def "should provide description"() {        given:
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
        and:
        assert description.contains("Composite function calculator")
        assert description.contains("quantity")
    }
    def "should fail during construction when calculator not found"() {        given:
        CalculatorId nonExistentId = CalculatorId.generate()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), nonExistentId)
        )
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { new CompositeFunctionCalculator("composite", ranges, repository) }

        assert exception.getMessage().contains("not found in repository")
    }
    def "should fail when component calculators have different interpretations"() {        given:
        repository.save(new SimpleFixedCalculator("total-calc", Money.of(100, "PLN"), Interpretation.TOTAL))
        repository.save(new SimpleFixedCalculator("unit-calc", Money.of(10, "PLN"), Interpretation.UNIT))

        CalculatorId totalId = repository.findByName("total-calc").get().getId()
        CalculatorId unitId = repository.findByName("unit-calc").get().getId()

        Ranges ranges = Ranges.of(
            "quantity",
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), totalId),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), unitId)
        )
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { new CompositeFunctionCalculator("composite", ranges, repository) }

        assert exception.getMessage().contains("same interpretation")
        assert exception.getMessage().contains("TOTAL")
        assert exception.getMessage().contains("UNIT")
    }
    def "should return shared interpretation of component calculators"() {        given:
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
        and:
        assert calculator.interpretation() == Interpretation.UNIT
    }
    def "should allow composite with all total price calculators"() {        given:
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
        and:
        assert calculator.interpretation() == Interpretation.TOTAL
        assert calculator != null
    }

    private static Throwable shouldFail(Class<? extends Throwable> type, Closure action) {
        try {
            action.call()
        } catch (Throwable exception) {
            assert type.isInstance(exception)
            return exception
        }
        throw new AssertionError("Expected " + type.simpleName)
    }
}

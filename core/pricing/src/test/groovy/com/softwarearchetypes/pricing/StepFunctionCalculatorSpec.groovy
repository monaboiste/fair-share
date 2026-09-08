package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.util.Map
import spock.lang.Specification


class StepFunctionCalculatorSpec extends Specification {
    def "shouldCalculateBasePriceForQuantityWithinFirstStep"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5")))
        and:
        Money result = calculator.calculate(params)
        and:
        assert new BigDecimal("100.00").compareTo(result.value()) == 0
    }
    def "shouldCalculateIncreasedPriceForSecondStep"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("15")))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("105.00").compareTo(result.value()) == 0
    }
    def "shouldCalculateIncreasedPriceForMultipleSteps"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("35")))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("115.00").compareTo(result.value()) == 0
    }
    def "shouldCalculatePriceAtExactStepBoundary"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("20")))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("110.00").compareTo(result.value()) == 0
    }
    def "shouldThrowExceptionWhenQuantityParameterMissing"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        Parameters params = Parameters.empty()
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldReturnCorrectType"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )
        and:
        assert calculator.getType() == CalculatorType.STEP_FUNCTION
    }
    def "shouldProvideDescription"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "Volume Pricing",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )
        and:
        String description = calculator.describe()
        and:
        assert description != null
        assert description.contains("100")
        assert description.contains("10")
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

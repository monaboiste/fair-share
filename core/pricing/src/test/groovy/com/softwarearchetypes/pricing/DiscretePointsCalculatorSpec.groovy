package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.util.HashMap
import java.util.Map
import spock.lang.Specification


class DiscretePointsCalculatorSpec extends Specification {
    def "shouldReturnPriceForDefinedQuantity"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))
        points.put(new BigDecimal("20"), Money.of(350, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("10")))
        and:
        Money result = calculator.calculate(params)
        and:
        assert new BigDecimal("180.00").compareTo(result.value()) == 0
    }
    def "shouldReturnCorrectPriceForAllDefinedPoints"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))
        points.put(new BigDecimal("20"), Money.of(350, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        and:
        Money result5 = calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("5"))))
        assert new BigDecimal("100.00").compareTo(result5.value()) == 0

        Money result10 = calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("10"))))
        assert new BigDecimal("180.00").compareTo(result10.value()) == 0

        Money result20 = calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("20"))))
        assert new BigDecimal("350.00").compareTo(result20.value()) == 0
    }
    def "shouldThrowExceptionForUndefinedQuantity"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))
        points.put(new BigDecimal("20"), Money.of(350, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("7")))
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { calculator.calculate(params) }

        assert exception.getMessage().contains("7")
        assert exception.getMessage().contains("not defined")
    }
    def "shouldThrowExceptionForQuantityOutsideRange"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("100")))
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldThrowExceptionWhenQuantityParameterMissing"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = Parameters.empty()
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldReturnCorrectType"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        and:
        assert calculator.getType() == CalculatorType.DISCRETE_POINTS
    }
    def "shouldProvideDescription"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        and:
        String description = calculator.describe()
        and:
        assert description != null
        assert description.contains("2")
        assert description.toLowerCase().contains("discrete")
    }
    def "shouldHandleSinglePoint"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("1"), Money.of(50, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Single Price", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("1")))
        and:
        Money result = calculator.calculate(params)
        and:
        assert new BigDecimal("50.00").compareTo(result.value()) == 0
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

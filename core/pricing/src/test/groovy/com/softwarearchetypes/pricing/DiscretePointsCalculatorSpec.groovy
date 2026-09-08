package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.util.HashMap
import java.util.Map
import spock.lang.Specification


class DiscretePointsCalculatorSpec extends Specification {
    def "price is returned for a defined quantity"() {
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
    def "correct price is returned for every defined point"() {
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
    def "undefined quantity raises an exception"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))
        points.put(new BigDecimal("20"), Money.of(350, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("7")))

        when:
        calculator.calculate(params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("7")
        ex.message.contains("not defined")
    }
    def "quantity outside all defined points raises an exception"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("100")))

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }
    def "missing quantity parameter raises an exception"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        Parameters params = Parameters.empty()

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }
    def "calculator type is discrete points"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)
        and:
        assert calculator.getType() == CalculatorType.DISCRETE_POINTS
    }
    def "description includes the number of defined points"() {
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
    def "single-point table returns that price"() {
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
}

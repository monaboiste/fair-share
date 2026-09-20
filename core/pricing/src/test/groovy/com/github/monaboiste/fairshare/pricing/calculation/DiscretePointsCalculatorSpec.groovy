package com.github.monaboiste.fairshare.pricing.calculation


import com.github.monaboiste.fairshare.quantity.money.Money
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
        PricingResult result = calculator.calculate(params)

        expect:
        new BigDecimal("180.00") == result.money().value()
    }

    def "correct price is returned for every defined point"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))
        points.put(new BigDecimal("20"), Money.of(350, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)

        and:
        PricingResult result5 = calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("5"))))

        expect:
        new BigDecimal("100.00") == result5.money().value()

        PricingResult result10 = calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("10"))))
        new BigDecimal("180.00") == result10.money().value()

        PricingResult result20 = calculator.calculate(new Parameters(Map.of("quantity", new BigDecimal("20"))))
        new BigDecimal("350.00") == result20.money().value()
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


    def "description includes the number of defined points"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("5"), Money.of(100, "PLN"))
        points.put(new BigDecimal("10"), Money.of(180, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Volume Discount", points)

        and:
        String description = calculator.describe()

        expect:
        description != null
        description.contains("2")
        description.toLowerCase().contains("discrete")
    }

    def "single-point table returns that price"() {
        given:
        Map<BigDecimal, Money> points = new HashMap<>()
        points.put(new BigDecimal("1"), Money.of(50, "PLN"))

        DiscretePointsCalculator calculator = new DiscretePointsCalculator("Single Price", points)
        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("1")))

        and:
        PricingResult result = calculator.calculate(params)

        expect:
        new BigDecimal("50.00") == result.money().value()
    }
}

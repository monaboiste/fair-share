package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Instant
import spock.lang.Specification

class ContinuousLinearTimeCalculatorSpec extends Specification {

    def "start price is returned at start time"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )

        Parameters params = new Parameters(Map.of("time", startTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("1999.00") == result.value()
    }

    def "end price is returned at end time"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )

        Parameters params = new Parameters(Map.of("time", endTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("3399.00") == result.value()
    }

    def "time before the start boundary raises an exception"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = Instant.parse("2024-05-31T12:00:00Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }

    def "time after the end boundary raises an exception"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = Instant.parse("2024-06-16T12:00:00Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }

    def "price is interpolated at the midpoint"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant midTime = Instant.parse("2024-06-08T00:00:00Z")
        Parameters params = new Parameters(Map.of("time", midTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("2699.00") == result.value()
    }

    def "price is interpolated at a half-day offset with precision"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = Instant.parse("2024-06-01T12:00:00Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("2049")

        expect:
        result.value().subtract(expected).abs() < new BigDecimal("1")
    }

    def "price is interpolated at the quarter point"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = Instant.parse("2024-06-04T12:00:00Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("2349")

        expect:
        result.value().subtract(expected).abs() < new BigDecimal("1")
    }

    def "price is interpolated at the three-quarters point"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = Instant.parse("2024-06-11T12:00:00Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("3049")

        expect:
        result.value().subtract(expected).abs() < new BigDecimal("1")
    }

    def "price is interpolated with minute precision"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T10:00:00Z")
        Instant endTime = Instant.parse("2024-06-01T11:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "minute-precision-test",
                startTime,
                Money.of(100, "PLN"),
                endTime,
                Money.of(200, "PLN")
        )
        Instant queryTime = Instant.parse("2024-06-01T10:30:00Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("150.00") == result.value()
    }

    def "interpolation works with different currencies"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing-eur",
                startTime,
                Money.of(199, "EUR"),
                endTime,
                Money.of(339, "EUR")
        )
        Instant midTime = Instant.parse("2024-06-08T00:00:00Z")
        Parameters params = new Parameters(Map.of("time", midTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("269.00") == result.value()
        result.toString().contains("EUR")
    }

    def "decreasing price is interpolated correctly"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "clearance-auction",
                startTime,
                Money.of(5000, "PLN"),
                endTime,
                Money.of(1000, "PLN")
        )
        Instant midTime = Instant.parse("2024-06-08T00:00:00Z")
        Parameters params = new Parameters(Map.of("time", midTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("3000.00") == result.value()
    }

    def "missing time parameter raises an exception"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )

        Parameters params = Parameters.empty()

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }

    def "calculator type is continuous linear time"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )

        expect:
        calculator.getType() == CalculatorType.CONTINUOUS_LINEAR_TIME
    }

    def "description includes start and end price and dates"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )

        and:
        String description = calculator.describe()

        expect:
        description != null
        description.contains("1999")
        description.contains("3399")
        description.contains("2024-06-01")
        description.contains("2024-06-15")
    }

    def "very short time intervals are interpolated correctly"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T10:00:00Z")
        Instant endTime = Instant.parse("2024-06-01T10:01:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "fast-auction",
                startTime,
                Money.of(100, "PLN"),
                endTime,
                Money.of(200, "PLN")
        )
        Instant queryTime = Instant.parse("2024-06-01T10:00:30Z")
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("150.00") == result.value()
    }

    def "formula shows linear interpolation expression"() {
        given:
        Instant startTime = Instant.parse("2024-06-01T00:00:00Z")
        Instant endTime = Instant.parse("2024-06-15T00:00:00Z")

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(new BigDecimal("1999"), "PLN"),
                endTime,
                Money.of(new BigDecimal("3399"), "PLN")
        )

        and:
        String formula = calculator.formula()

        and:
        String expected = ("f(t) = startPrice + progress \u00d7 (endPrice - startPrice)%n" +
                "where progress = (t - startTime) / (endTime - startTime)%n" +
                "domain: t \u2208 [2024-06-01T00:00:00Z, 2024-06-15T00:00:00Z]").formatted()

        expect:
        formula == expected
    }
}

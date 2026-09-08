package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Instant
import spock.lang.Specification

class ContinuousLinearTimeCalculatorSpec extends Specification {

    def "start price is returned at start time"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

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
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

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
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = instant(2024, 5, 31, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }

    def "time after the end boundary raises an exception"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = instant(2024, 6, 16, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }

    def "price is interpolated at the midpoint"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant midTime = instant(2024, 6, 8, 0, 0)
        Parameters params = new Parameters(Map.of("time", midTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("2699.00") == result.value()
    }

    def "price is interpolated at a half-day offset with precision"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = instant(2024, 6, 1, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("2049")

        expect:
        result.value().subtract(expected).abs() < new BigDecimal("1")
    }

    def "price is interpolated at the quarter point"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = instant(2024, 6, 4, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("2349")

        expect:
        result.value().subtract(expected).abs() < new BigDecimal("1")
    }

    def "price is interpolated at the three-quarters point"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        Instant queryTime = instant(2024, 6, 11, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("3049")

        expect:
        result.value().subtract(expected).abs() < new BigDecimal("1")
    }

    def "price is interpolated with minute precision"() {
        given:
        Instant startTime = instant(2024, 6, 1, 10, 0)
        Instant endTime = instant(2024, 6, 1, 11, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "minute-precision-test",
                startTime,
                Money.of(100, "PLN"),
                endTime,
                Money.of(200, "PLN")
        )
        Instant queryTime = instant(2024, 6, 1, 10, 30)
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("150.00") == result.value()
    }

    def "interpolation works with different currencies"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing-eur",
                startTime,
                Money.of(199, "EUR"),
                endTime,
                Money.of(339, "EUR")
        )
        Instant midTime = instant(2024, 6, 8, 0, 0)
        Parameters params = new Parameters(Map.of("time", midTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("269.00") == result.value()
        result.toString().contains("EUR")
    }

    def "decreasing price is interpolated correctly"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "clearance-auction",
                startTime,
                Money.of(5000, "PLN"),
                endTime,
                Money.of(1000, "PLN")
        )
        Instant midTime = instant(2024, 6, 8, 0, 0)
        Parameters params = new Parameters(Map.of("time", midTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("3000.00") == result.value()
    }

    def "missing time parameter raises an exception"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

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
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

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
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

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
        Instant startTime = instant(2024, 6, 1, 10, 0, 0)
        Instant endTime = instant(2024, 6, 1, 10, 1, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "fast-auction",
                startTime,
                Money.of(100, "PLN"),
                endTime,
                Money.of(200, "PLN")
        )
        Instant queryTime = instant(2024, 6, 1, 10, 0, 30)
        Parameters params = new Parameters(Map.of("time", queryTime))

        and:
        Money result = calculator.calculate(params)

        expect:
        new BigDecimal("150.00") == result.value()
    }

    def "formula shows linear interpolation expression"() {
        given:
        Instant startTime = instant(2024, 6, 1, 0, 0)
        Instant endTime = instant(2024, 6, 15, 0, 0)

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

    private static Instant instant(int year, int month, int day, int hour, int minute) {
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC)
    }

    private static Instant instant(int year, int month, int day, int hour, int minute, int second) {
        LocalDateTime.of(year, month, day, hour, minute, second).toInstant(ZoneOffset.UTC)
    }

}

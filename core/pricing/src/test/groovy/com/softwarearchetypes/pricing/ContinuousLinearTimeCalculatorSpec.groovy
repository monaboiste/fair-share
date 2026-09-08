package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Map
import spock.lang.Specification


class ContinuousLinearTimeCalculatorSpec extends Specification {
    def "shouldReturnStartPriceAtStartTime"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )

        Parameters params = new Parameters(Map.of("time", startTime))
        given:
        Money result = calculator.calculate(params)
        and:
        assert new BigDecimal("1999.00").compareTo(result.value()) == 0
    }
    def "shouldReturnEndPriceAtEndTime"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

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
        and:
        assert new BigDecimal("3399.00").compareTo(result.value()) == 0
    }
    def "shouldThrowExceptionForTimeBeforeStart"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 5, 31, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldThrowExceptionForTimeAfterEnd"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 16, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldInterpolateAtMidpoint"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        LocalDateTime midTime = LocalDateTime.of(2024, 6, 8, 0, 0)
        Parameters params = new Parameters(Map.of("time", midTime))
        given:
        Money result = calculator.calculate(params)
        assert new BigDecimal("2699.00").compareTo(result.value()) == 0
    }
    def "shouldInterpolateAtHalfDayWithPrecision"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 1, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))
        given:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("2049")
        assert result.value().subtract(expected).abs().compareTo(new BigDecimal("1")) < 0
    }
    def "shouldInterpolatePreciselyAtQuarterPoint"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 4, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))
        given:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("2349")
        assert result.value().subtract(expected).abs().compareTo(new BigDecimal("1")) < 0
    }
    def "shouldInterpolateAtThreeQuartersPoint"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 11, 12, 0)
        Parameters params = new Parameters(Map.of("time", queryTime))
        given:
        Money result = calculator.calculate(params)
        BigDecimal expected = new BigDecimal("3049")
        assert result.value().subtract(expected).abs().compareTo(new BigDecimal("1")) < 0
    }
    def "shouldInterpolateWithMinutePrecision"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 10, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 1, 11, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "minute-precision-test",
            startTime,
            Money.of(100, "PLN"),
            endTime,
            Money.of(200, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 1, 10, 30)
        Parameters params = new Parameters(Map.of("time", queryTime))
        given:
        Money result = calculator.calculate(params)
        assert new BigDecimal("150.00").compareTo(result.value()) == 0
    }
    def "shouldWorkWithDifferentCurrencies"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing-eur",
            startTime,
            Money.of(199, "EUR"),
            endTime,
            Money.of(339, "EUR")
        )
        LocalDateTime midTime = LocalDateTime.of(2024, 6, 8, 0, 0)
        Parameters params = new Parameters(Map.of("time", midTime))
        given:
        Money result = calculator.calculate(params)
        assert new BigDecimal("269.00").compareTo(result.value()) == 0
        assert result.toString().contains("EUR")
    }
    def "shouldHandleDecreasingPrice"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "clearance-auction",
            startTime,
            Money.of(5000, "PLN"),
            endTime,
            Money.of(1000, "PLN")
        )
        LocalDateTime midTime = LocalDateTime.of(2024, 6, 8, 0, 0)
        Parameters params = new Parameters(Map.of("time", midTime))
        given:
        Money result = calculator.calculate(params)
        assert new BigDecimal("3000.00").compareTo(result.value()) == 0
    }
    def "shouldThrowExceptionWhenTimeParameterMissing"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )

        Parameters params = Parameters.empty()
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldReturnCorrectType"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        and:
        assert calculator.getType() == CalculatorType.CONTINUOUS_LINEAR_TIME
    }
    def "shouldProvideDescription"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        and:
        String description = calculator.describe()
        and:
        assert description != null
        assert description.contains("1999")
        assert description.contains("3399")
        assert description.contains("2024-06-01")
        assert description.contains("2024-06-15")
    }
    def "shouldHandleVeryShortTimeInterval"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 10, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 1, 10, 1, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "fast-auction",
            startTime,
            Money.of(100, "PLN"),
            endTime,
            Money.of(200, "PLN")
        )
        LocalDateTime queryTime = LocalDateTime.of(2024, 6, 1, 10, 0, 30)
        Parameters params = new Parameters(Map.of("time", queryTime))
        given:
        Money result = calculator.calculate(params)
        assert new BigDecimal("150.00").compareTo(result.value()) == 0
    }
    def "shouldProvideFormula"() {
        given:
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

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
        String expected = "f(t) = startPrice + progress × (endPrice - startPrice)\n" +
                         "where progress = (t - startTime) / (endTime - startTime)\n" +
                         "domain: t ∈ [2024-06-01T00:00, 2024-06-15T00:00]"
        assert formula == expected
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

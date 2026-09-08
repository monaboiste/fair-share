package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Map
import spock.lang.Specification


class DailyIncrementCalculatorSpec extends Specification {
    def "shouldCalculatePriceAtStartDate"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 1)))
        and:
        Money result = calculator.calculate(params)
        and:
        assert new BigDecimal("1999.00").compareTo(result.value()) == 0
    }
    def "shouldCalculatePriceAfterOneDay"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 2)))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("2099.00").compareTo(result.value()) == 0
    }
    def "shouldCalculatePriceAfterSevenDays"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 8)))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("2699.00").compareTo(result.value()) == 0
    }
    def "shouldCalculatePriceAfterFourteenDays"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 15)))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("3399.00").compareTo(result.value()) == 0
    }
    def "shouldCalculatePriceBeforeStartDate"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )
        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 5, 31)))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("1899.00").compareTo(result.value()) == 0
    }
    def "shouldWorkWithDifferentCurrencies"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing-eur",
            LocalDate.of(2024, 6, 1),
            Money.of(199, "EUR"),
            Money.of(10, "EUR")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 8)))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("269.00").compareTo(result.value()) == 0
        assert result.toString().contains("EUR")
    }
    def "shouldThrowExceptionWhenDateParameterMissing"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )

        Parameters params = Parameters.empty()
        and:
        shouldFail(IllegalArgumentException) { calculator.calculate(params) }
    }
    def "shouldReturnCorrectType"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )
        and:
        assert calculator.getType() == CalculatorType.DAILY_INCREMENT
    }
    def "shouldProvideDescription"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )
        and:
        String description = calculator.describe()
        and:
        assert description != null
        assert description.contains("1999")
        assert description.contains("100")
        assert description.contains("2024-06-01")
    }
    def "shouldHandleDecreasingPriceWithNegativeIncrement"() {
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "clearance-sale",
            LocalDate.of(2024, 6, 1),
            Money.of(5000, "PLN"),
            Money.of(-100, "PLN")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 11)))
        given:
        Money result = calculator.calculate(params)
        assert new BigDecimal("4000.00").compareTo(result.value()) == 0
    }
    def "shouldProvideFormula"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(new BigDecimal("1999"), "PLN"),
            Money.of(new BigDecimal("100"), "PLN")
        )
        and:
        String formula = calculator.formula()
        and:
        String expected = "f(date) = startPrice + daysFromStart × dailyIncrement\n" +
                         "where:\n" +
                         "  startDate = 2024-06-01\n" +
                         "  startPrice = PLN 1999\n" +
                         "  dailyIncrement = PLN 100"
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

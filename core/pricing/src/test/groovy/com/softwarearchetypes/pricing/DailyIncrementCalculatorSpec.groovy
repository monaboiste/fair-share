package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Map
import spock.lang.Specification


class DailyIncrementCalculatorSpec extends Specification {
    def "start date returns the start price"() {
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
    def "one day after start returns start price plus one increment"() {
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
    def "seven days after start returns start price plus seven increments"() {
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
    def "fourteen days after start returns the accumulated price"() {
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
    def "date before start returns start price minus the elapsed decrement"() {
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
    def "daily increment works with different currencies"() {
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
    def "missing date parameter raises an exception"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "presale-pricing",
            LocalDate.of(2024, 6, 1),
            Money.of(1999, "PLN"),
            Money.of(100, "PLN")
        )

        Parameters params = Parameters.empty()

        when:
        calculator.calculate(params)

        then:
        thrown(IllegalArgumentException)
    }
    def "calculator type is daily increment"() {
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
    def "description includes start price, increment, and date"() {
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
    def "negative daily increment decreases the price over time"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "clearance-sale",
            LocalDate.of(2024, 6, 1),
            Money.of(5000, "PLN"),
            Money.of(-100, "PLN")
        )

        Parameters params = new Parameters(Map.of("date", LocalDate.of(2024, 6, 11)))
        and:
        Money result = calculator.calculate(params)
        assert new BigDecimal("4000.00").compareTo(result.value()) == 0
    }
    def "formula shows daily increment expression"() {
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
        String expected = "f(date) = startPrice + daysFromStart \u00d7 dailyIncrement\n" +
                         "where:\n" +
                         "  startDate = 2024-06-01\n" +
                         "  startPrice = PLN 1999\n" +
                         "  dailyIncrement = PLN 100"
        assert formula == expected
    }
}

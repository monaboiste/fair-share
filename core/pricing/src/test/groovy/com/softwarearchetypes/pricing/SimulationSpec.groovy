package com.softwarearchetypes.pricing

import static java.time.Clock.fixed

import com.softwarearchetypes.quantity.money.Money
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class SimulationSpec extends Specification {

    def "step function calculator can be simulated over a quantity range"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
                "volume-discount",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5")
        )
        List<Parameters> points = new ArrayList<>()
        for (int qty in (0..30).step(5)) {
            points.add(Parameters.of("quantity", new BigDecimal(qty)))
        }
        and:
        Map<Parameters, Money> results = calculator.simulate(points)

        expect:
        results != null
        results.size() == 7
        new BigDecimal("100.00") == results.get(Parameters.of("quantity", BigDecimal.ZERO)).value()
        new BigDecimal("100.00") == results.get(Parameters.of("quantity", new BigDecimal("5"))).value()
        new BigDecimal("105.00") == results.get(Parameters.of("quantity", new BigDecimal("10"))).value()
        new BigDecimal("105.00") == results.get(Parameters.of("quantity", new BigDecimal("15"))).value()
        new BigDecimal("110.00") == results.get(Parameters.of("quantity", new BigDecimal("20"))).value()
        new BigDecimal("110.00") == results.get(Parameters.of("quantity", new BigDecimal("25"))).value()
        new BigDecimal("115.00") == results.get(Parameters.of("quantity", new BigDecimal("30"))).value()
    }

    def "daily increment calculator can be simulated over a date range"() {
        given:
        LocalDate startDate = LocalDate.of(2024, 6, 1)
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
                "presale-pricing",
                startDate,
                Money.of(1999, "PLN"),
                Money.of(100, "PLN")
        )
        List<Parameters> points = new ArrayList<>()
        for (int day in (0..14).step(2)) {
            points.add(Parameters.of("date", startDate.plusDays(day)))
        }
        and:
        Map<Parameters, Money> results = calculator.simulate(points)

        expect:
        results != null
        results.size() == 8

        new BigDecimal("1999.00") == results.get(Parameters.of("date", startDate)).value()
        new BigDecimal("2199.00") == results.get(Parameters.of("date", startDate.plusDays(2))).value()
        new BigDecimal("2399.00") == results.get(Parameters.of("date", startDate.plusDays(4))).value()
        new BigDecimal("2599.00") == results.get(Parameters.of("date", startDate.plusDays(6))).value()
        new BigDecimal("2799.00") == results.get(Parameters.of("date", startDate.plusDays(8))).value()
        new BigDecimal("2999.00") == results.get(Parameters.of("date", startDate.plusDays(10))).value()
        new BigDecimal("3199.00") == results.get(Parameters.of("date", startDate.plusDays(12))).value()
        new BigDecimal("3399.00") == results.get(Parameters.of("date", startDate.plusDays(14))).value()
    }

    def "continuous linear time calculator can be simulated"() {
        given:
        Instant startTime = LocalDateTime.of(2024, 6, 1, 0, 0).toInstant(ZoneOffset.UTC)
        Instant endTime = LocalDateTime.of(2024, 6, 15, 0, 0).toInstant(ZoneOffset.UTC)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "auction-pricing",
                startTime,
                Money.of(1999, "PLN"),
                endTime,
                Money.of(3399, "PLN")
        )
        List<Parameters> points = List.of(
                Parameters.of("time", startTime),
                Parameters.of("time", startTime.plus(3, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS)),
                Parameters.of("time", startTime.plus(7, ChronoUnit.DAYS)),
                Parameters.of("time", startTime.plus(10, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS)),
                Parameters.of("time", endTime)
        )

        and:
        Map<Parameters, Money> results = calculator.simulate(points)

        and:
        Money at25Percent = results.get(Parameters.of("time", startTime.plus(3, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS)))
        Money at75Percent = results.get(Parameters.of("time", startTime.plus(10, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS)))

        expect:
        results != null
        results.size() == 5
        new BigDecimal("1999.00") == results.get(Parameters.of("time", startTime)).value()
        new BigDecimal("2699.00") == results.get(Parameters.of("time", startTime.plus(7, ChronoUnit.DAYS))).value()
        new BigDecimal("3399.00") == results.get(Parameters.of("time", endTime)).value()
        new BigDecimal("2349") == at25Percent.value().setScale(0, RoundingMode.HALF_UP)

        new BigDecimal("3049") == at75Percent.value().setScale(0, RoundingMode.HALF_UP)
    }

    def "simple fixed calculator always returns the same price when simulated"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator(
                "flat-fee",
                Money.of(50, "PLN")
        )
        List<Parameters> points = List.of(
                Parameters.empty(),
                Parameters.of("quantity", new BigDecimal("1")),
                Parameters.of("quantity", new BigDecimal("100")),
                Parameters.of("anything", "value")
        )

        and:
        Map<Parameters, Money> results = calculator.simulate(points)

        expect:
        results != null
        results.size() == 4

        results.values().each { price ->
            new BigDecimal("50.00") == price.value()
        }
    }

    def "discrete points calculator can be simulated over its defined points"() {
        given:
        Map<BigDecimal, Money> pricePoints = Map.of(
                new BigDecimal("5"), Money.of(100, "PLN"),
                new BigDecimal("10"), Money.of(180, "PLN"),
                new BigDecimal("20"), Money.of(350, "PLN")
        )

        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
                "bulk-pricing",
                pricePoints
        )
        List<Parameters> points = List.of(
                Parameters.of("quantity", new BigDecimal("5")),
                Parameters.of("quantity", new BigDecimal("10")),
                Parameters.of("quantity", new BigDecimal("20"))
        )

        and:
        Map<Parameters, Money> results = calculator.simulate(points)

        expect:
        results != null
        results.size() == 3

        new BigDecimal("100.00") == results.get(Parameters.of("quantity", new BigDecimal("5"))).value()
        new BigDecimal("180.00") == results.get(Parameters.of("quantity", new BigDecimal("10"))).value()
        new BigDecimal("350.00") == results.get(Parameters.of("quantity", new BigDecimal("20"))).value()
    }

    def "composite calculator can be simulated"() {
        given:
        Instant NOW = LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant()
        Clock clock = fixed(NOW, ZoneId.systemDefault())
        PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()

        Calculator lowTier = facade.addCalculator(
                "low-tier",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(20, "PLN"))
        )

        Calculator mediumTier = facade.addCalculator(
                "medium-tier",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(10, "PLN"))
        )

        Calculator highTier = facade.addCalculator(
                "high-tier",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(0, "PLN"))
        )

        List<CalculatorRange> ranges = List.of(
                new NumericRange(BigDecimal.ZERO, new BigDecimal("1000"), lowTier.getId()),
                new NumericRange(new BigDecimal("1000"), new BigDecimal("4000"), mediumTier.getId()),
                new NumericRange(new BigDecimal("4000"), new BigDecimal(Integer.MAX_VALUE), highTier.getId())
        )

        CompositeFunctionCalculator composite = (CompositeFunctionCalculator) facade.addCalculator(
                "account-fee",
                CalculatorType.COMPOSITE,
                Parameters.of(
                        "rangeSelector", "monthlyIncome",
                        "ranges", ranges
                )
        )
        List<Parameters> points = List.of(
                Parameters.of("monthlyIncome", BigDecimal.ZERO),
                Parameters.of("monthlyIncome", new BigDecimal("500")),
                Parameters.of("monthlyIncome", new BigDecimal("1000")),
                Parameters.of("monthlyIncome", new BigDecimal("2500")),
                Parameters.of("monthlyIncome", new BigDecimal("4000")),
                Parameters.of("monthlyIncome", new BigDecimal("10000"))
        )

        and:
        Map<Parameters, Money> results = composite.simulate(points)

        expect:
        results != null
        results.size() == 6
        new BigDecimal("20.00") == results.get(Parameters.of("monthlyIncome", BigDecimal.ZERO)).value()
        new BigDecimal("20.00") == results.get(Parameters.of("monthlyIncome", new BigDecimal("500"))).value()
        new BigDecimal("10.00") == results.get(Parameters.of("monthlyIncome", new BigDecimal("1000"))).value()
        new BigDecimal("10.00") == results.get(Parameters.of("monthlyIncome", new BigDecimal("2500"))).value()
        BigDecimal.ZERO == results.get(Parameters.of("monthlyIncome", new BigDecimal("4000"))).value()
        BigDecimal.ZERO == results.get(Parameters.of("monthlyIncome", new BigDecimal("10000"))).value()
    }

    def "simulation results preserve input order"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
                "test",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5")
        )
        List<Parameters> points = List.of(
                Parameters.of("quantity", new BigDecimal("30")),
                Parameters.of("quantity", new BigDecimal("10")),
                Parameters.of("quantity", new BigDecimal("20")),
                Parameters.of("quantity", BigDecimal.ZERO)
        )

        and:
        Map<Parameters, Money> results = calculator.simulate(points)
        List<Parameters> resultKeys = new ArrayList<>(results.keySet())

        expect:
        resultKeys.get(0) == points.get(0)
        resultKeys.get(1) == points.get(1)
        resultKeys.get(2) == points.get(2)
        resultKeys.get(3) == points.get(3)
    }

    def "simulating with an empty list returns an empty result"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator(
                "test",
                Money.of(100, "PLN")
        )

        and:
        Map<Parameters, Money> results = calculator.simulate(List.of())

        expect:
        results != null
        results.size() == 0
    }
}

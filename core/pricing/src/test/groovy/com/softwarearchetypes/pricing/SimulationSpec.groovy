package com.softwarearchetypes.pricing

import static java.time.Clock.fixed

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.ArrayList
import java.util.List
import java.util.Map
import spock.lang.Specification


class SimulationSpec extends Specification {
    def "shouldSimulateStepFunctionCalculatorOverQuantityRange"() {
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
        given:
        Map<Parameters, Money> results = calculator.simulate(points)
        assert results != null
        assert results.size() == 7
        assert new BigDecimal("100.00").compareTo(
            results.get(Parameters.of("quantity", BigDecimal.ZERO)).value()) == 0
        assert new BigDecimal("100.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("5"))).value()) == 0
        assert new BigDecimal("105.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("10"))).value()) == 0
        assert new BigDecimal("105.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("15"))).value()) == 0
        assert new BigDecimal("110.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("20"))).value()) == 0
        assert new BigDecimal("110.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("25"))).value()) == 0
        assert new BigDecimal("115.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("30"))).value()) == 0
    }
    def "shouldSimulateDailyIncrementCalculatorOverDateRange"() {
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
        given:
        Map<Parameters, Money> results = calculator.simulate(points)
        assert results != null
        assert results.size() == 8

        assert new BigDecimal("1999.00").compareTo(
            results.get(Parameters.of("date", startDate)).value()) == 0
        assert new BigDecimal("2199.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(2))).value()) == 0
        assert new BigDecimal("2399.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(4))).value()) == 0
        assert new BigDecimal("2599.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(6))).value()) == 0
        assert new BigDecimal("2799.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(8))).value()) == 0
        assert new BigDecimal("2999.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(10))).value()) == 0
        assert new BigDecimal("3199.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(12))).value()) == 0
        assert new BigDecimal("3399.00").compareTo(
            results.get(Parameters.of("date", startDate.plusDays(14))).value()) == 0
    }
    def "shouldSimulateContinuousLinearTimeCalculator"() {
        LocalDateTime startTime = LocalDateTime.of(2024, 6, 1, 0, 0)
        LocalDateTime endTime = LocalDateTime.of(2024, 6, 15, 0, 0)

        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "auction-pricing",
            startTime,
            Money.of(1999, "PLN"),
            endTime,
            Money.of(3399, "PLN")
        )
        List<Parameters> points = List.of(
            Parameters.of("time", startTime),
            Parameters.of("time", startTime.plusDays(3).plusHours(12)),
            Parameters.of("time", startTime.plusDays(7)),
            Parameters.of("time", startTime.plusDays(10).plusHours(12)),
            Parameters.of("time", endTime)
        )
        given:
        Map<Parameters, Money> results = calculator.simulate(points)
        assert results != null
        assert results.size() == 5
        assert new BigDecimal("1999.00").compareTo(
            results.get(Parameters.of("time", startTime)).value()) == 0
        assert new BigDecimal("2699.00").compareTo(
            results.get(Parameters.of("time", startTime.plusDays(7))).value()) == 0
        assert new BigDecimal("3399.00").compareTo(
            results.get(Parameters.of("time", endTime)).value()) == 0
        Money at25Percent = results.get(Parameters.of("time", startTime.plusDays(3).plusHours(12)))
        assert new BigDecimal("2349").compareTo(at25Percent.value().setScale(0, java.math.RoundingMode.HALF_UP)) == 0
        Money at75Percent = results.get(Parameters.of("time", startTime.plusDays(10).plusHours(12)))
        assert new BigDecimal("3049").compareTo(at75Percent.value().setScale(0, java.math.RoundingMode.HALF_UP)) == 0
    }
    def "shouldSimulateSimpleFixedCalculator"() {
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
        given:
        Map<Parameters, Money> results = calculator.simulate(points)
        assert results != null
        assert results.size() == 4

        results.values().each { price ->
            assert new BigDecimal("50.00").compareTo(price.value()) == 0
        }
    }
    def "shouldSimulateDiscretePointsCalculator"() {
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
        given:
        Map<Parameters, Money> results = calculator.simulate(points)
        and:
        assert results != null
        assert results.size() == 3

        assert new BigDecimal("100.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("5"))).value()) == 0
        assert new BigDecimal("180.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("10"))).value()) == 0
        assert new BigDecimal("350.00").compareTo(
            results.get(Parameters.of("quantity", new BigDecimal("20"))).value()) == 0
    }
    def "shouldSimulateCompositeCalculator"() {
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
        given:
        Map<Parameters, Money> results = composite.simulate(points)
        and:
        assert results != null
        assert results.size() == 6
        assert new BigDecimal("20.00").compareTo(
            results.get(Parameters.of("monthlyIncome", BigDecimal.ZERO)).value()) == 0
        assert new BigDecimal("20.00").compareTo(
            results.get(Parameters.of("monthlyIncome", new BigDecimal("500"))).value()) == 0
        assert new BigDecimal("10.00").compareTo(
            results.get(Parameters.of("monthlyIncome", new BigDecimal("1000"))).value()) == 0
        assert new BigDecimal("10.00").compareTo(
            results.get(Parameters.of("monthlyIncome", new BigDecimal("2500"))).value()) == 0
        assert BigDecimal.ZERO.compareTo(
            results.get(Parameters.of("monthlyIncome", new BigDecimal("4000"))).value()) == 0
        assert BigDecimal.ZERO.compareTo(
            results.get(Parameters.of("monthlyIncome", new BigDecimal("10000"))).value()) == 0
    }
    def "shouldPreserveOrderInSimulationResults"() {
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
        assert resultKeys.get(0) == points.get(0)
        assert resultKeys.get(1) == points.get(1)
        assert resultKeys.get(2) == points.get(2)
        assert resultKeys.get(3) == points.get(3)
    }
    def "shouldSimulateEmptyListOfPoints"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator(
            "test",
            Money.of(100, "PLN")
        )
        and:
        Map<Parameters, Money> results = calculator.simulate(List.of())
        and:
        assert results != null
        assert results.size() == 0
    }
}

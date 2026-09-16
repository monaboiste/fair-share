package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.LocalDateTime
import spock.lang.Specification

class AdaptersSpec extends Specification {

    def "unit-to-total adapter multiplies by quantity"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator totalCalculator = UnitToTotalAdapter.wrap("adapter", unitCalculator)
        PricingResult total = totalCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        total.money() == Money.of(150, "PLN")
        totalCalculator.interpretation() == Interpretation.TOTAL
    }

    def "unit-to-total adapter rejects a non-unit-price calculator"() {
        given:
        Calculator totalCalculator = new SimpleFixedCalculator(
                "total",
                Money.of(100, "PLN"),
                Interpretation.TOTAL
        )

        when:
        UnitToTotalAdapter.wrap("adapter", totalCalculator)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("UNIT")
    }

    def "unit-to-marginal adapter returns the same price regardless of quantity"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator marginalCalculator = UnitToMarginalAdapter.wrap("adapter", unitCalculator)
        PricingResult marginal5 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))
        PricingResult marginal15 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        marginal5.money() == Money.of(10, "PLN")
        marginal15.money() == Money.of(10, "PLN")
        marginalCalculator.interpretation() == Interpretation.MARGINAL
    }

    def "total-to-unit adapter divides total by quantity"() {
        given:
        Calculator totalCalculator = new StepFunctionCalculator(
                "bulk-pricing",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.TOTAL
        )
        Calculator unitCalculator = TotalToUnitAdapter.wrap("adapter", totalCalculator)
        PricingResult unit = unitCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        unit.money() == Money.of(7, "PLN")
        unitCalculator.interpretation() == Interpretation.UNIT
    }

    def "total-to-marginal adapter computes the marginal as the derivative"() {
        given:
        Calculator totalCalculator = new StepFunctionCalculator(
                "step-pricing",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.TOTAL
        )
        Calculator marginalCalculator = TotalToMarginalAdapter.wrap("adapter", totalCalculator)
        PricingResult marginal1 = marginalCalculator.calculate(Parameters.of("quantity", BigDecimal.ONE))

        expect:
        marginal1.money() == Money.of(100, "PLN")
        PricingResult marginal10 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("10")))
        marginal10.money() == Money.of(5, "PLN")
        PricingResult marginal11 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("11")))
        marginal11.money() == Money.of(0, "PLN")

        marginalCalculator.interpretation() == Interpretation.MARGINAL
    }

    def "marginal-to-total adapter sums marginal prices"() {
        given:
        Calculator marginalCalculator = new SimpleFixedCalculator(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator totalCalculator = MarginalToTotalAdapter.wrap("adapter", marginalCalculator)
        PricingResult total = totalCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))

        expect:
        total.money() == Money.of(50, "PLN")
        totalCalculator.interpretation() == Interpretation.TOTAL
    }

    def "marginal-to-unit adapter returns the average price"() {
        given:
        Calculator marginalCalculator = new SimpleFixedCalculator(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator unitCalculator = MarginalToUnitAdapter.wrap("adapter", marginalCalculator)
        PricingResult unit = unitCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))

        expect:
        unit.money() == Money.of(10, "PLN")
        unitCalculator.interpretation() == Interpretation.UNIT
    }

    def "unit-to-marginal adapter works for a variable unit price"() {
        given:
        Calculator unitCalculator = new StepFunctionCalculator(
                "bulk-unit-price",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.UNIT
        )
        Calculator marginalCalculator = UnitToMarginalAdapter.wrap("adapter", unitCalculator)
        PricingResult marginal6 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("6")))
        PricingResult marginal11 = marginalCalculator.calculate(Parameters.of("quantity", new BigDecimal("11")))

        expect:
        marginal6 != null
        marginal11 != null
        marginalCalculator.interpretation() == Interpretation.MARGINAL
    }

    def "each adapter reports its correct calculator type"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator("u", Money.of(10, "PLN"), Interpretation.UNIT)
        Calculator totalCalculator = new SimpleFixedCalculator("t", Money.of(100, "PLN"), Interpretation.TOTAL)
        Calculator marginalCalculator = new SimpleFixedCalculator("m", Money.of(10, "PLN"), Interpretation.MARGINAL)

        expect:
        UnitToTotalAdapter.wrap("a", unitCalculator).getType() == CalculatorType.UNIT_TO_TOTAL_ADAPTER
        UnitToMarginalAdapter.wrap("a", unitCalculator).getType() == CalculatorType.UNIT_TO_MARGINAL_ADAPTER
        TotalToUnitAdapter.wrap("a", totalCalculator).getType() == CalculatorType.TOTAL_TO_UNIT_ADAPTER
        TotalToMarginalAdapter.wrap("a", totalCalculator).getType() == CalculatorType.TOTAL_TO_MARGINAL_ADAPTER
        MarginalToTotalAdapter.wrap("a", marginalCalculator).getType() == CalculatorType.MARGINAL_TO_TOTAL_ADAPTER
        MarginalToUnitAdapter.wrap("a", marginalCalculator).getType() == CalculatorType.MARGINAL_TO_UNIT_ADAPTER
    }

    def "derived adapter evaluations retain unrelated source parameters"() {
        given:
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 3, 4)
        Money baseAmount = Money.of(10, "PLN")
        List evaluations = [
                [UnitToTotalAdapter.&wrap, Interpretation.UNIT, [new BigDecimal("2")]],
                [UnitToMarginalAdapter.&wrap, Interpretation.UNIT, [new BigDecimal("2"), BigDecimal.ONE]],
                [TotalToUnitAdapter.&wrap, Interpretation.TOTAL, [new BigDecimal("2")]],
                [TotalToMarginalAdapter.&wrap, Interpretation.TOTAL, [new BigDecimal("2"), BigDecimal.ONE]],
                [MarginalToTotalAdapter.&wrap, Interpretation.MARGINAL, [BigDecimal.ONE, new BigDecimal("2")]],
                [MarginalToUnitAdapter.&wrap, Interpretation.MARGINAL, [BigDecimal.ONE, new BigDecimal("2")]]
        ].collect { adapter, interpretation, expectedQuantities ->
            def source = new TrackingCalculator(interpretation)
            def wrapped = adapter("adapter", source)
            wrapped.calculate(Parameters.of(
                    "quantity", new BigDecimal("2"),
                    "baseAmount", baseAmount,
                    "timestamp", timestamp
            ))
            [source, expectedQuantities]
        }

        expect:
        evaluations.every { source, expectedQuantities ->
            source.received*.get("baseAmount").every { it == baseAmount } &&
                    source.received*.get("timestamp").every { it == timestamp } &&
                    source.received*.get("quantity") == expectedQuantities
        }
    }

    def "adapter collapses a compatible quantity descriptor from its source calculator"() {
        given:
        def source = new TrackingCalculator(Interpretation.UNIT, new ParameterKey<>("quantity", BigDecimal.class))
        def adapter = UnitToTotalAdapter.wrap("adapter", source)

        expect:
        adapter.inputs().count { it.name() == "quantity" } == 1
        adapter.calculate(Parameters.of(
                "quantity", new BigDecimal("2"),
                "baseAmount", Money.of(10, "PLN"),
                "timestamp", LocalDateTime.of(2026, 1, 2, 3, 4)
        )).money() == Money.of(40, "PLN")
    }

    def "adapter rejects an incompatible quantity descriptor before evaluating its source"() {
        given:
        def source = new TrackingCalculator(Interpretation.UNIT, new ParameterKey<>("quantity", Money.class))
        def adapter = UnitToTotalAdapter.wrap("adapter", source)

        when:
        adapter.calculate(Parameters.of(
                "quantity", new BigDecimal("2"),
                "baseAmount", Money.of(10, "PLN"),
                "timestamp", LocalDateTime.of(2026, 1, 2, 3, 4)
        ))

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("quantity")
        ex.message.contains("BigDecimal")
        ex.message.contains("Money")
        source.received.empty
    }

    def "adapter formula includes the source calculator formula"() {
        given:
        Calculator unitCalculator = new SimpleFixedCalculator("test", Money.of(10, "PLN"), Interpretation.UNIT)

        Calculator totalAdapter = UnitToTotalAdapter.wrap("adapter", unitCalculator)

        String formula = totalAdapter.formula()

        expect:
        formula.contains("quantity \u00d7")
        formula.contains("f(x) = PLN 10")
    }

    private static class TrackingCalculator implements Calculator {
        private final Interpretation interpretation
        private final ParameterDefinition quantity
        final List<Parameters> received = []

        TrackingCalculator(Interpretation interpretation, ParameterDefinition quantity = new ParameterKey<>("quantity", BigDecimal.class)) {
            this.interpretation = interpretation
            this.quantity = quantity
        }

        @Override
        Set<ParameterDefinition> inputs() {
            [quantity, new ParameterKey<>("baseAmount", Money.class), new ParameterKey<>("timestamp", LocalDateTime.class)] as Set
        }

        @Override
        PricingResult calculateWithValidInputs(Parameters parameters) {
            received << parameters
            Calculator.result(interpretation, parameters.getMoney("baseAmount").multiply(parameters.getBigDecimal("quantity")))
        }

        @Override
        CalculatorType getType() { CalculatorType.CUSTOM }

        @Override
        CalculatorId getId() { CalculatorId.generate() }

        @Override
        String name() { "tracking" }

        @Override
        String describe() { "tracking" }

        @Override
        String formula() { "tracking" }

        @Override
        Interpretation interpretation() { interpretation }
    }
}

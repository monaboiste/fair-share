package com.github.monaboiste.fairshare.pricing.component

import com.github.monaboiste.fairshare.pricing.calculation.Calculator
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId
import com.github.monaboiste.fairshare.pricing.calculation.Calculators
import com.github.monaboiste.fairshare.pricing.calculation.Interpretation
import com.github.monaboiste.fairshare.pricing.calculation.MarginalPrice
import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult
import com.github.monaboiste.fairshare.pricing.calculation.TotalPrice
import com.github.monaboiste.fairshare.pricing.calculation.UnitPrice
import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.LocalDateTime
import spock.lang.Specification

class AdaptersSpec extends Specification {

    def "unit-to-total adapter multiplies by quantity"() {
        given:
        Calculator unitCalculator = Calculators.fixed(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator totalCalculator = UnitToTotalAdapter.wrap("adapter", unitCalculator)
        PricingResult total = totalCalculator.calculate(Parameters.of("quantity", new BigDecimal("15")))

        expect:
        total.money() == Money.of(150, "PLN")
    }


    def "unit-to-marginal adapter returns the same price regardless of quantity"() {
        given:
        Calculator unitCalculator = Calculators.fixed(
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
    }

    def "total-to-unit adapter divides total by quantity"() {
        given:
        Calculator totalCalculator = Calculators.stepFunction(
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
    }

    def "total-to-marginal adapter computes the marginal as the derivative"() {
        given:
        Calculator totalCalculator = Calculators.stepFunction(
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
    }

    def "marginal-to-total adapter sums marginal prices"() {
        given:
        Calculator marginalCalculator = Calculators.fixed(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator totalCalculator = MarginalToTotalAdapter.wrap("adapter", marginalCalculator)
        PricingResult total = totalCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))

        expect:
        total.money() == Money.of(50, "PLN")
    }

    def "marginal-to-unit adapter returns the average price"() {
        given:
        Calculator marginalCalculator = Calculators.fixed(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator unitCalculator = MarginalToUnitAdapter.wrap("adapter", marginalCalculator)
        PricingResult unit = unitCalculator.calculate(Parameters.of("quantity", new BigDecimal("5")))

        expect:
        unit.money() == Money.of(10, "PLN")
    }

    def "unit-to-marginal adapter works for a variable unit price"() {
        given:
        Calculator unitCalculator = Calculators.stepFunction(
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
        marginal6.money() == Money.of(100, "PLN")
        marginal11.money() == Money.of(105, "PLN")
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

    def "marginal adapters reject quantities below one"() {
        given:
        Calculator total = Calculators.fixed("total", Money.of(10, "PLN"), Interpretation.TOTAL)
        Calculator unit = Calculators.fixed("unit", Money.of(10, "PLN"), Interpretation.UNIT)
        def toMarginalAdapter = new TotalToMarginalAdapter(CalculatorId.generate(), "adapter", total)
        def unitToMarginalAdapter = new UnitToMarginalAdapter(CalculatorId.generate(), "adapter", unit)

        when:
        toMarginalAdapter.calculate(Parameters.of("quantity", new BigDecimal("0.5")))

        then:
        thrown(IllegalArgumentException)

        when:
        unitToMarginalAdapter.calculate(Parameters.of("quantity", BigDecimal.ZERO))

        then:
        thrown(IllegalArgumentException)
    }

    def "adapter metadata describes each conversion"() {
        given:
        Calculator source = Calculators.fixed("source", Money.of(10, "PLN"), interpretation)
        Calculator adapted = InterpretationAdapters.adapt(source, target, Parameters.empty())

        expect:
        adapted.name().contains(target.name().toLowerCase())
        adapted.describe()
        adapted.formula()
        adapted.getId() != null

        where:
        interpretation        | target
        Interpretation.UNIT   | Interpretation.TOTAL
        Interpretation.UNIT   | Interpretation.MARGINAL
        Interpretation.TOTAL  | Interpretation.UNIT
        Interpretation.TOTAL  | Interpretation.MARGINAL
        Interpretation.MARGINAL | Interpretation.TOTAL
        Interpretation.MARGINAL | Interpretation.UNIT
    }

    def "adapter formula includes the source calculator formula"() {
        given:
        Calculator unitCalculator = Calculators.fixed("test", Money.of(10, "PLN"), Interpretation.UNIT)

        Calculator totalAdapter = UnitToTotalAdapter.wrap("adapter", unitCalculator)

        String formula = totalAdapter.formula()

        expect:
        formula.contains("quantity \u00d7")
        formula.contains("f(x) = PLN 10")
    }

    private static class TrackingCalculator implements Calculator {
        private final Interpretation interpretation
        final List<Parameters> received = []

        TrackingCalculator(Interpretation interpretation) {
            this.interpretation = interpretation
        }

        @Override
        PricingResult calculate(Parameters parameters) {
            received << parameters
            def money = parameters.getMoney("baseAmount") * parameters.getBigDecimal("quantity")
            if (interpretation == Interpretation.TOTAL) {
                return new TotalPrice(money)
            }
            if (interpretation == Interpretation.UNIT) {
                return new UnitPrice(money)
            }
            return new MarginalPrice(money)
        }

        @Override
        CalculatorId getId() { CalculatorId.generate() }

        @Override
        String name() { "tracking" }

        @Override
        String describe() { "tracking" }

        @Override
        String formula() { "tracking" }

    }
}

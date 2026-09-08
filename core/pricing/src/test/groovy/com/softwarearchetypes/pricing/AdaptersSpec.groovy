package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import spock.lang.Specification


class AdaptersSpec extends Specification {
    def "unitToTotalAdapter shouldMultiplyByQuantity"() {
        given:
        Calculator unitCalc = new SimpleFixedCalculator(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator totalCalc = UnitToTotalAdapter.wrap("adapter", unitCalc)
        Money total = totalCalc.calculate(Parameters.of("quantity", new BigDecimal("15")))
        assert total == Money.of(150, "PLN")
        assert totalCalc.interpretation() == Interpretation.TOTAL
    }
    def "unitToTotalAdapter shouldRejectNonUnitPriceCalculator"() {
        given:
        Calculator totalCalc = new SimpleFixedCalculator(
                "total",
                Money.of(100, "PLN"),
                Interpretation.TOTAL
        )
        IllegalArgumentException ex = shouldFail(IllegalArgumentException) { UnitToTotalAdapter.wrap("adapter", totalCalc) }
        assert ex.getMessage().contains("UNIT")
    }
    def "unitToMarginalAdapter shouldReturnSamePrice"() {
        given:
        Calculator unitCalc = new SimpleFixedCalculator(
                "unit-price",
                Money.of(10, "PLN"),
                Interpretation.UNIT
        )
        Calculator marginalCalc = UnitToMarginalAdapter.wrap("adapter", unitCalc)
        Money marginal5 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("5")))
        Money marginal15 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("15")))

        assert marginal5 == Money.of(10, "PLN")
        assert marginal15 == Money.of(10, "PLN")
        assert marginalCalc.interpretation() == Interpretation.MARGINAL
    }
    def "totalToUnitAdapter shouldDivideByQuantity"() {
        given:
        Calculator totalCalc = new StepFunctionCalculator(
                "bulk-pricing",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.TOTAL
        )
        Calculator unitCalc = TotalToUnitAdapter.wrap("adapter", totalCalc)
        Money unit = unitCalc.calculate(Parameters.of("quantity", new BigDecimal("15")))
        assert unit == Money.of(7, "PLN")
        assert unitCalc.interpretation() == Interpretation.UNIT
    }
    def "totalToMarginalAdapter shouldCalculateDerivative"() {
        given:
        Calculator totalCalc = new StepFunctionCalculator(
                "step-pricing",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.TOTAL
        )
        Calculator marginalCalc = TotalToMarginalAdapter.wrap("adapter", totalCalc)
        Money marginal1 = marginalCalc.calculate(Parameters.of("quantity", BigDecimal.ONE))
        assert marginal1 == Money.of(100, "PLN")
        Money marginal10 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("10")))
        assert marginal10 == Money.of(5, "PLN")
        Money marginal11 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("11")))
        assert marginal11 == Money.of(0, "PLN")

        assert marginalCalc.interpretation() == Interpretation.MARGINAL
    }
    def "marginalToTotalAdapter shouldSumMarginalPrices"() {
        given:
        Calculator marginalCalc = new SimpleFixedCalculator(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator totalCalc = MarginalToTotalAdapter.wrap("adapter", marginalCalc)
        Money total = totalCalc.calculate(Parameters.of("quantity", new BigDecimal("5")))
        assert total == Money.of(50, "PLN")
        assert totalCalc.interpretation() == Interpretation.TOTAL
    }
    def "marginalToUnitAdapter shouldCalculateAverage"() {
        given:
        Calculator marginalCalc = new SimpleFixedCalculator(
                "marginal-price",
                Money.of(10, "PLN"),
                Interpretation.MARGINAL
        )
        Calculator unitCalc = MarginalToUnitAdapter.wrap("adapter", marginalCalc)
        Money unit = unitCalc.calculate(Parameters.of("quantity", new BigDecimal("5")))
        assert unit == Money.of(10, "PLN")
        assert unitCalc.interpretation() == Interpretation.UNIT
    }
    def "unitToMarginalAdapter shouldWorkForVariableUnitPrice"() {
        given:
        Calculator unitCalc = new StepFunctionCalculator(
                "bulk-unit-price",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5"),
                Interpretation.UNIT
        )
        Calculator marginalCalc = UnitToMarginalAdapter.wrap("adapter", unitCalc)
        Money marginal6 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("6")))
        Money marginal11 = marginalCalc.calculate(Parameters.of("quantity", new BigDecimal("11")))
        assert marginal6 != null
        assert marginal11 != null
        assert marginalCalc.interpretation() == Interpretation.MARGINAL
    }
    def "adapters shouldHaveCorrectTypes"() {
        given:
        Calculator unitCalc = new SimpleFixedCalculator("u", Money.of(10, "PLN"), Interpretation.UNIT)
        Calculator totalCalc = new SimpleFixedCalculator("t", Money.of(100, "PLN"), Interpretation.TOTAL)
        Calculator marginalCalc = new SimpleFixedCalculator("m", Money.of(10, "PLN"), Interpretation.MARGINAL)

        assert UnitToTotalAdapter.wrap("a", unitCalc).getType() == CalculatorType.UNIT_TO_TOTAL_ADAPTER
        assert UnitToMarginalAdapter.wrap("a", unitCalc).getType() == CalculatorType.UNIT_TO_MARGINAL_ADAPTER
        assert TotalToUnitAdapter.wrap("a", totalCalc).getType() == CalculatorType.TOTAL_TO_UNIT_ADAPTER
        assert TotalToMarginalAdapter.wrap("a", totalCalc).getType() == CalculatorType.TOTAL_TO_MARGINAL_ADAPTER
        assert MarginalToTotalAdapter.wrap("a", marginalCalc).getType() == CalculatorType.MARGINAL_TO_TOTAL_ADAPTER
        assert MarginalToUnitAdapter.wrap("a", marginalCalc).getType() == CalculatorType.MARGINAL_TO_UNIT_ADAPTER
    }
    def "adapters shouldPreserveSourceCalculatorFormula"() {
        given:
        Calculator unitCalc = new SimpleFixedCalculator("test", Money.of(10, "PLN"), Interpretation.UNIT)

        Calculator totalAdapter = UnitToTotalAdapter.wrap("adapter", unitCalc)

        String formula = totalAdapter.formula()
        assert formula.contains("quantity ×")
        assert formula.contains("f(x) = PLN 10")
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

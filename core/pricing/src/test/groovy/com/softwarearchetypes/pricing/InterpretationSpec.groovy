package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Map
import spock.lang.Specification


class InterpretationSpec extends Specification {
    def "simple fixed calculator defaults to total interpretation"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator("test", Money.of(100, "PLN"))

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "simple fixed calculator can be set to a specific interpretation"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator(
            "test",
            Money.of(100, "PLN"),
            Interpretation.UNIT
        )

        assert calculator.interpretation() == Interpretation.UNIT
    }
    def "step function calculator defaults to total interpretation"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "test",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "step function calculator can be set to a specific interpretation"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "test",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5"),
            Interpretation.MARGINAL
        )

        assert calculator.interpretation() == Interpretation.MARGINAL
    }
    def "discrete points calculator defaults to total interpretation"() {
        given:
        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
            "test",
            Map.of(new BigDecimal("5"), Money.of(100, "PLN"))
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "discrete points calculator can be set to a specific interpretation"() {
        given:
        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
            "test",
            Map.of(new BigDecimal("5"), Money.of(100, "PLN")),
            Interpretation.UNIT
        )

        assert calculator.interpretation() == Interpretation.UNIT
    }
    def "daily increment calculator defaults to total interpretation"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "test",
            LocalDate.of(2024, 1, 1),
            Money.of(100, "PLN"),
            Money.of(10, "PLN")
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "daily increment calculator can be set to a specific interpretation"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "test",
            LocalDate.of(2024, 1, 1),
            Money.of(100, "PLN"),
            Money.of(10, "PLN"),
            Interpretation.MARGINAL
        )

        assert calculator.interpretation() == Interpretation.MARGINAL
    }
    def "continuous linear time calculator defaults to total interpretation"() {
        given:
        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "test",
            LocalDateTime.of(2024, 1, 1, 0, 0),
            Money.of(100, "PLN"),
            LocalDateTime.of(2024, 1, 10, 0, 0),
            Money.of(200, "PLN")
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "continuous linear time calculator can be set to a specific interpretation"() {
        given:
        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
            "test",
            LocalDateTime.of(2024, 1, 1, 0, 0),
            Money.of(100, "PLN"),
            LocalDateTime.of(2024, 1, 10, 0, 0),
            Money.of(200, "PLN"),
            Interpretation.UNIT
        )

        assert calculator.interpretation() == Interpretation.UNIT
    }
    def "each interpretation has a human-readable description"() {
        given:
        assert Interpretation.TOTAL.describe() == "Total price for entire quantity/period"
        assert Interpretation.UNIT.describe() == "Average price per single unit"
        assert Interpretation.MARGINAL.describe() == "Price of n-th specific unit"
    }
}

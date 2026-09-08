package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Map
import spock.lang.Specification


class InterpretationSpec extends Specification {
    def "shouldHaveTotalAsDefaultInterpretationForSimpleFixed"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator("test", Money.of(100, "PLN"))

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "shouldAllowSettingInterpretationForSimpleFixed"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator(
            "test",
            Money.of(100, "PLN"),
            Interpretation.UNIT
        )

        assert calculator.interpretation() == Interpretation.UNIT
    }
    def "shouldHaveTotalAsDefaultInterpretationForStepFunction"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
            "test",
            Money.of(100, "PLN"),
            new BigDecimal("10"),
            new BigDecimal("5")
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "shouldAllowSettingInterpretationForStepFunction"() {
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
    def "shouldHaveTotalAsDefaultInterpretationForDiscretePoints"() {
        given:
        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
            "test",
            Map.of(new BigDecimal("5"), Money.of(100, "PLN"))
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "shouldAllowSettingInterpretationForDiscretePoints"() {
        given:
        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
            "test",
            Map.of(new BigDecimal("5"), Money.of(100, "PLN")),
            Interpretation.UNIT
        )

        assert calculator.interpretation() == Interpretation.UNIT
    }
    def "shouldHaveTotalAsDefaultInterpretationForDailyIncrement"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
            "test",
            LocalDate.of(2024, 1, 1),
            Money.of(100, "PLN"),
            Money.of(10, "PLN")
        )

        assert calculator.interpretation() == Interpretation.TOTAL
    }
    def "shouldAllowSettingInterpretationForDailyIncrement"() {
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
    def "shouldHaveTotalAsDefaultInterpretationForContinuousLinearTime"() {
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
    def "shouldAllowSettingInterpretationForContinuousLinearTime"() {
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
    def "shouldProvideDescriptionsForInterpretations"() {
        given:
        assert Interpretation.TOTAL.describe() == "Total price for entire quantity/period"
        assert Interpretation.UNIT.describe() == "Average price per single unit"
        assert Interpretation.MARGINAL.describe() == "Price of n-th specific unit"
    }
}

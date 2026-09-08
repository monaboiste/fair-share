package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import spock.lang.Specification

class InterpretationSpec extends Specification {

    def "simple fixed calculator defaults to total interpretation"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator("test", Money.of(100, "PLN"))

        expect:
        calculator.interpretation() == Interpretation.TOTAL
    }

    def "simple fixed calculator can be set to a specific interpretation"() {
        given:
        SimpleFixedCalculator calculator = new SimpleFixedCalculator(
                "test",
                Money.of(100, "PLN"),
                Interpretation.UNIT
        )

        expect:
        calculator.interpretation() == Interpretation.UNIT
    }

    def "step function calculator defaults to total interpretation"() {
        given:
        StepFunctionCalculator calculator = new StepFunctionCalculator(
                "test",
                Money.of(100, "PLN"),
                new BigDecimal("10"),
                new BigDecimal("5")
        )

        expect:
        calculator.interpretation() == Interpretation.TOTAL
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

        expect:
        calculator.interpretation() == Interpretation.MARGINAL
    }

    def "discrete points calculator defaults to total interpretation"() {
        given:
        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
                "test",
                Map.of(new BigDecimal("5"), Money.of(100, "PLN"))
        )

        expect:
        calculator.interpretation() == Interpretation.TOTAL
    }

    def "discrete points calculator can be set to a specific interpretation"() {
        given:
        DiscretePointsCalculator calculator = new DiscretePointsCalculator(
                "test",
                Map.of(new BigDecimal("5"), Money.of(100, "PLN")),
                Interpretation.UNIT
        )

        expect:
        calculator.interpretation() == Interpretation.UNIT
    }

    def "daily increment calculator defaults to total interpretation"() {
        given:
        DailyIncrementCalculator calculator = new DailyIncrementCalculator(
                "test",
                LocalDate.of(2024, 1, 1),
                Money.of(100, "PLN"),
                Money.of(10, "PLN")
        )

        expect:
        calculator.interpretation() == Interpretation.TOTAL
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

        expect:
        calculator.interpretation() == Interpretation.MARGINAL
    }

    def "continuous linear time calculator defaults to total interpretation"() {
        given:
        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "test",
                instant(2024, 1, 1, 0, 0),
                Money.of(100, "PLN"),
                instant(2024, 1, 10, 0, 0),
                Money.of(200, "PLN")
        )

        expect:
        calculator.interpretation() == Interpretation.TOTAL
    }

    def "continuous linear time calculator can be set to a specific interpretation"() {
        given:
        ContinuousLinearTimeCalculator calculator = new ContinuousLinearTimeCalculator(
                "test",
                instant(2024, 1, 1, 0, 0),
                Money.of(100, "PLN"),
                instant(2024, 1, 10, 0, 0),
                Money.of(200, "PLN"),
                Interpretation.UNIT
        )

        expect:
        calculator.interpretation() == Interpretation.UNIT
    }

    def "each interpretation has a human-readable description"() {

        expect:
        Interpretation.TOTAL.describe() == "Total price for entire quantity/period"
        Interpretation.UNIT.describe() == "Average price per single unit"
        Interpretation.MARGINAL.describe() == "Price of n-th specific unit"
    }

    private static Instant instant(int year, int month, int day, int hour, int minute) {
        LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC)
    }

    private static Instant instant(int year, int month, int day, int hour, int minute, int second) {
        LocalDateTime.of(year, month, day, hour, minute, second).toInstant(ZoneOffset.UTC)
    }

}

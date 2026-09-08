package com.softwarearchetypes.pricing

import java.time.LocalDate
import java.time.LocalTime
import spock.lang.Specification



class DateRangeSpec extends Specification {
    def "should support local date values"() {        given:
        DateRange range = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )
        and:
        assert range.supports(LocalDate.of(2024, 7, 15))
        assert !(range.supports("2024-07-15"))
        assert !(range.supports(2024))
    }
    def "should contain date in range"() {
        DateRange range = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )
        given:
        assert range.contains(LocalDate.of(2024, 6, 1))
        assert range.contains(LocalDate.of(2024, 7, 15))
        assert range.contains(LocalDate.of(2024, 8, 31))
        assert !(range.contains(LocalDate.of(2024, 9, 1)))
        assert !(range.contains(LocalDate.of(2024, 5, 31)))
        assert !(range.contains(LocalDate.of(2024, 9, 2)))
    }
    def "should not contain date of wrong type"() {        given:
        DateRange range = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )
        and:
        assert !(range.contains("2024-07-15"))
        assert !(range.contains(2024))
    }
    def "should throw when from not before to"() {        given:
        shouldFail(IllegalArgumentException) {
                CalculatorRange.date(
                        LocalDate.of(2024, 9, 1),
                        LocalDate.of(2024, 6, 1),
                        CalculatorId.generate()
                ) }
    }
    def "should throw when from equals to"() {        given:
        shouldFail(IllegalArgumentException) {
                CalculatorRange.date(
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 6, 1),
                        CalculatorId.generate()
                ) }
    }
    def "should detect overlap when ranges overlap"() {        given:
        DateRange summer = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )
        DateRange lateSummer = CalculatorRange.date(
                LocalDate.of(2024, 8, 1),
                LocalDate.of(2024, 10, 1),
                CalculatorId.generate()
        )
        and:
        assert summer.overlaps(lateSummer)
        assert lateSummer.overlaps(summer)
    }
    def "should not detect overlap when ranges adjacent"() {        given:
        DateRange summer = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )
        DateRange fall = CalculatorRange.date(
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 12, 1),
                CalculatorId.generate()
        )
        and:
        assert !(summer.overlaps(fall))
        assert !(fall.overlaps(summer))
    }
    def "should detect overlap when one range contains another"() {        given:
        DateRange year = CalculatorRange.date(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 1, 1),
                CalculatorId.generate()
        )
        DateRange summer = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )
        and:
        assert year.overlaps(summer)
        assert summer.overlaps(year)
    }
    def "should be compatible with other date ranges"() {        given:
        DateRange range1 = CalculatorRange.date(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 1),
                CalculatorId.generate()
        )
        DateRange range2 = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 12, 1),
                CalculatorId.generate()
        )
        and:
        assert range1.isCompatibleWith(range2)
    }
    def "should not be compatible with time range"() {        given:
        DateRange dateRange = CalculatorRange.date(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 1),
                CalculatorId.generate()
        )
        TimeRange timeRange = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        and:
        assert !(dateRange.isCompatibleWith(timeRange))
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

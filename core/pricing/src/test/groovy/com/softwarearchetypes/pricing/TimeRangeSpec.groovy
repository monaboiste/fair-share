package com.softwarearchetypes.pricing

import java.time.LocalTime
import spock.lang.Specification



class TimeRangeSpec extends Specification {
    def "should support local time values"() {        given:
        TimeRange range = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        and:
        assert range.supports(LocalTime.of(12, 0))
        assert !(range.supports("12:00"))
        assert !(range.supports(12))
    }
    def "should contain time in normal range"() {
        TimeRange range = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        given:
        assert range.contains(LocalTime.of(8, 0))
        assert range.contains(LocalTime.of(12, 0))
        assert range.contains(LocalTime.of(17, 59))
        assert !(range.contains(LocalTime.of(18, 0)))
        assert !(range.contains(LocalTime.of(7, 59)))
        assert !(range.contains(LocalTime.of(18, 1)))
    }
    def "should contain time in range crossing midnight"() {
        TimeRange range = CalculatorRange.time(
            LocalTime.of(22, 0),
            LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        given:
        assert range.contains(LocalTime.of(22, 0))
        assert range.contains(LocalTime.of(23, 30))
        assert range.contains(LocalTime.of(0, 0))
        assert range.contains(LocalTime.of(3, 0))
        assert range.contains(LocalTime.of(5, 59))
        assert !(range.contains(LocalTime.of(6, 0)))
        assert !(range.contains(LocalTime.of(12, 0)))
        assert !(range.contains(LocalTime.of(18, 0)))
    }
    def "should not contain time of wrong type"() {        given:
        TimeRange range = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        and:
        assert !(range.contains("12:00"))
        assert !(range.contains(12))
    }
    def "should detect overlap when both ranges normal and overlap"() {        given:
        TimeRange range1 = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        TimeRange range2 = CalculatorRange.time(
            LocalTime.of(15, 0),
            LocalTime.of(20, 0),
                CalculatorId.generate()
        )
        and:
        assert range1.overlaps(range2)
        assert range2.overlaps(range1)
    }
    def "should not detect overlap when both ranges normal and adjacent"() {        given:
        TimeRange range1 = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        TimeRange range2 = CalculatorRange.time(
            LocalTime.of(18, 0),
            LocalTime.of(22, 0),
                CalculatorId.generate()
        )
        and:
        assert !(range1.overlaps(range2))
        assert !(range2.overlaps(range1))
    }
    def "should not detect overlap when one crosses midnight and other fits in gap"() {        given:
        TimeRange night = CalculatorRange.time(
            LocalTime.of(22, 0),
            LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        TimeRange day = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        and:
        assert !(night.overlaps(day))
        assert !(day.overlaps(night))
    }
    def "should detect overlap when one crosses midnight and other overlaps"() {        given:
        TimeRange night = CalculatorRange.time(
            LocalTime.of(22, 0),
            LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        TimeRange lateEvening = CalculatorRange.time(
            LocalTime.of(20, 0),
            LocalTime.of(23, 0),
                CalculatorId.generate()
        )
        and:
        assert night.overlaps(lateEvening)
        assert lateEvening.overlaps(night)
    }
    def "should detect overlap when both cross midnight"() {        given:
        TimeRange night1 = CalculatorRange.time(
            LocalTime.of(22, 0),
            LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        TimeRange night2 = CalculatorRange.time(
            LocalTime.of(20, 0),
            LocalTime.of(8, 0),
                CalculatorId.generate()
        )
        and:
        assert night1.overlaps(night2)
        assert night2.overlaps(night1)
    }
    def "should be compatible with other time ranges"() {        given:
        TimeRange range1 = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        TimeRange range2 = CalculatorRange.time(
            LocalTime.of(18, 0),
            LocalTime.of(22, 0),
                CalculatorId.generate()
        )
        and:
        assert range1.isCompatibleWith(range2)
    }
    def "should not be compatible with numeric range"() {        given:
        TimeRange timeRange = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        NumericRange numericRange = CalculatorRange.numeric(
            java.math.BigDecimal.ZERO,
            java.math.BigDecimal.TEN,
                CalculatorId.generate()
        )
        and:
        assert !(timeRange.isCompatibleWith(numericRange))
    }
    def "should throw when checking overlap with incompatible range"() {        given:
        TimeRange timeRange = CalculatorRange.time(
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        NumericRange numericRange = CalculatorRange.numeric(
            java.math.BigDecimal.ZERO,
            java.math.BigDecimal.TEN,
                CalculatorId.generate()
        )
        and:
        shouldFail(IllegalArgumentException) {
            timeRange.overlaps(numericRange) }
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

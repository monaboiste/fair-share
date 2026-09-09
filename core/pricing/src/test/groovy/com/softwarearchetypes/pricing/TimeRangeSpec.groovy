package com.softwarearchetypes.pricing

import java.time.LocalTime
import spock.lang.Specification

class TimeRangeSpec extends Specification {

    def "supports local time values"() {
        given:
        CalculatorRange range = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )

        expect:
        range.supports(LocalTime.of(12, 0))
        !(range.supports("12:00"))
        !(range.supports(12))
    }

    def "contains time in normal range"() {
        CalculatorRange range = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )

        expect:
        range.contains(LocalTime.of(8, 0))
        range.contains(LocalTime.of(12, 0))
        range.contains(LocalTime.of(17, 59))
        !(range.contains(LocalTime.of(18, 0)))
        !(range.contains(LocalTime.of(7, 59)))
        !(range.contains(LocalTime.of(18, 1)))
    }

    def "contains time in range crossing midnight"() {
        CalculatorRange range = CalculatorRange.time(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                CalculatorId.generate()
        )

        expect:
        range.contains(LocalTime.of(22, 0))
        range.contains(LocalTime.of(23, 30))
        range.contains(LocalTime.of(0, 0))
        range.contains(LocalTime.of(3, 0))
        range.contains(LocalTime.of(5, 59))
        !(range.contains(LocalTime.of(6, 0)))
        !(range.contains(LocalTime.of(12, 0)))
        !(range.contains(LocalTime.of(18, 0)))
    }

    def "does not contain time of wrong type"() {
        given:
        CalculatorRange range = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )

        expect:
        !(range.contains("12:00"))
        !(range.contains(12))
    }

    def "detects overlap when both ranges normal and overlap"() {
        given:
        CalculatorRange range1 = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        CalculatorRange range2 = CalculatorRange.time(
                LocalTime.of(15, 0),
                LocalTime.of(20, 0),
                CalculatorId.generate()
        )

        expect:
        range1.overlaps(range2)
        range2.overlaps(range1)
    }

    def "does not detect overlap when both ranges normal and adjacent"() {
        given:
        CalculatorRange range1 = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        CalculatorRange range2 = CalculatorRange.time(
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                CalculatorId.generate()
        )

        expect:
        !(range1.overlaps(range2))
        !(range2.overlaps(range1))
    }

    def "does not detect overlap when one crosses midnight and other fits in gap"() {
        given:
        CalculatorRange night = CalculatorRange.time(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        CalculatorRange day = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )

        expect:
        !(night.overlaps(day))
        !(day.overlaps(night))
    }

    def "detects overlap when one crosses midnight and other overlaps"() {
        given:
        CalculatorRange night = CalculatorRange.time(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        CalculatorRange lateEvening = CalculatorRange.time(
                LocalTime.of(20, 0),
                LocalTime.of(23, 0),
                CalculatorId.generate()
        )

        expect:
        night.overlaps(lateEvening)
        lateEvening.overlaps(night)
    }

    def "detects overlap when both cross midnight"() {
        given:
        CalculatorRange night1 = CalculatorRange.time(
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                CalculatorId.generate()
        )
        CalculatorRange night2 = CalculatorRange.time(
                LocalTime.of(20, 0),
                LocalTime.of(8, 0),
                CalculatorId.generate()
        )

        expect:
        night1.overlaps(night2)
        night2.overlaps(night1)
    }

    def "is compatible with other time ranges"() {
        given:
        CalculatorRange range1 = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        CalculatorRange range2 = CalculatorRange.time(
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                CalculatorId.generate()
        )

        expect:
        range1.isCompatibleWith(range2)
    }

    def "is not compatible with numeric range"() {
        given:
        CalculatorRange timeRange = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        NumericRange numericRange = CalculatorRange.numeric(
                BigDecimal.ZERO,
                BigDecimal.TEN,
                CalculatorId.generate()
        )

        expect:
        !(timeRange.isCompatibleWith(numericRange))
    }

    def "throws when checking overlap with incompatible range"() {
        given:
        CalculatorRange timeRange = CalculatorRange.time(
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                CalculatorId.generate()
        )
        CalculatorRange numericRange = CalculatorRange.numeric(
                BigDecimal.ZERO,
                BigDecimal.TEN,
                CalculatorId.generate()
        )

        when:
        timeRange.overlaps(numericRange)

        then:
        thrown(IllegalArgumentException)
    }
}

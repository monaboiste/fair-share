package com.softwarearchetypes.pricing

import java.time.LocalDate
import java.time.LocalTime
import spock.lang.Specification

class DateRangeSpec extends Specification {

    def "supports local date values"() {
        given:
        DateRange range = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )

        expect:
        range.supports(LocalDate.of(2024, 7, 15))
        !(range.supports("2024-07-15"))
        !(range.supports(2024))
    }

    def "contains date in range"() {
        DateRange range = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )

        expect:
        range.contains(LocalDate.of(2024, 6, 1))
        range.contains(LocalDate.of(2024, 7, 15))
        range.contains(LocalDate.of(2024, 8, 31))
        !(range.contains(LocalDate.of(2024, 9, 1)))
        !(range.contains(LocalDate.of(2024, 5, 31)))
        !(range.contains(LocalDate.of(2024, 9, 2)))
    }

    def "does not contain date of wrong type"() {
        given:
        DateRange range = CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 9, 1),
                CalculatorId.generate()
        )

        expect:
        !(range.contains("2024-07-15"))
        !(range.contains(2024))
    }

    def "throws when from not before to"() {

        when:
        CalculatorRange.date(
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 6, 1),
                CalculatorId.generate()
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "throws when from equals to"() {

        when:
        CalculatorRange.date(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 1),
                CalculatorId.generate()
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "detects overlap when ranges overlap"() {
        given:
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

        expect:
        summer.overlaps(lateSummer)
        lateSummer.overlaps(summer)
    }

    def "does not detect overlap when ranges adjacent"() {
        given:
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

        expect:
        !(summer.overlaps(fall))
        !(fall.overlaps(summer))
    }

    def "detects overlap when one range contains another"() {
        given:
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

        expect:
        year.overlaps(summer)
        summer.overlaps(year)
    }

    def "is compatible with other date ranges"() {
        given:
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

        expect:
        range1.isCompatibleWith(range2)
    }

    def "is not compatible with time range"() {
        given:
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

        expect:
        !(dateRange.isCompatibleWith(timeRange))
    }
}

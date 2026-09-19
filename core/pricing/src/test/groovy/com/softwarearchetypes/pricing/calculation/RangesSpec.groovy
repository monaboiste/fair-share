package com.softwarearchetypes.pricing.calculation

import java.time.LocalDate
import java.time.LocalTime
import spock.lang.Specification

class RangesSpec extends Specification {

    def "creates ranges with valid non overlapping ranges"() {
        given:
        List<CalculatorRange> rangesList = List.of(
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), CalculatorId.generate()),
                CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), CalculatorId.generate())
        )

        and:
        Ranges ranges = new Ranges("quantity", rangesList)

        expect:
        ranges.size() == 3
    }

    def "throws when ranges are empty"() {
        when:
        new Ranges("quantity", List.of())

        then:
        thrown(IllegalArgumentException)
    }

    def "throws when range selector is blank"() {
        given:
        List<CalculatorRange> rangesList = List.of(
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate())
        )

        when:
        new Ranges("  ", rangesList)

        then:
        thrown(IllegalArgumentException)
    }

    def "throws when ranges overlap"() {
        given:
        List<CalculatorRange> rangesList = List.of(
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                CalculatorRange.numeric(new BigDecimal("5"), new BigDecimal("15"), CalculatorId.generate())
        )

        when:
        new Ranges("quantity", rangesList)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("overlap")
    }


    def "finds matching range for numeric string value"() {
        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
                "quantity",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), matchingRangeCalculatorId)
                )
        )

        expect:
        ranges.findMatching(Parameters.of("quantity", "25")).orElseThrow().calculatorId() == matchingRangeCalculatorId
    }

    def "finds matching range for numeric value"() {
        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
                "quantity",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), matchingRangeCalculatorId),
                        CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), CalculatorId.generate())
                )
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("25")))

        and:
        CalculatorRange matchingRange = ranges.findMatching(params).orElseThrow()

        expect:
        matchingRange.calculatorId() == matchingRangeCalculatorId
    }

    def "finds matching range for date string value"() {
        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
                "date",
                List.of(
                        CalculatorRange.date(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1), matchingRangeCalculatorId)
                )
        )

        expect:
        ranges.findMatching(Parameters.of("date", "2025-06-01")).orElseThrow().calculatorId() == matchingRangeCalculatorId
    }

    def "finds matching range for time value"() {
        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
                "time",
                List.of(
                        CalculatorRange.time(LocalTime.of(8, 0), LocalTime.of(18, 0), matchingRangeCalculatorId),
                        CalculatorRange.time(LocalTime.of(18, 0), LocalTime.of(8, 0), CalculatorId.generate())
                )
        )

        Parameters params = new Parameters(Map.of("time", LocalTime.of(15, 30)))

        and:
        CalculatorRange matchingRange = ranges.findMatching(params).orElseThrow()

        expect:
        matchingRange.calculatorId() == matchingRangeCalculatorId
    }

    def "returns empty when no matching range"() {
        given:
        Ranges ranges = new Ranges(
                "quantity",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), CalculatorId.generate())
                )
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5")))

        and:
        var result = ranges.findMatching(params)

        expect:
        result.isEmpty()
    }

    def "throws when parameter not found"() {
        given:
        Ranges ranges = new Ranges(
                "quantity",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate())
                )
        )

        Parameters params = new Parameters(Map.of("weight", new BigDecimal("5")))

        when:
        ranges.findMatching(params)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("quantity")
        ex.message.contains("required")
    }

    def "finds first matching range when adjacent"() {
        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
                "quantity",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("20"), matchingRangeCalculatorId)
                )
        )

        Parameters paramsAt10 = new Parameters(Map.of("quantity", new BigDecimal("10")))

        and:
        CalculatorRange matchingRange = ranges.findMatching(paramsAt10).orElseThrow()

        expect:
        matchingRange.calculatorId() == matchingRangeCalculatorId
    }
}

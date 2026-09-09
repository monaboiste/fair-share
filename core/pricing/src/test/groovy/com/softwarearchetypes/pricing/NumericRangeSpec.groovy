package com.softwarearchetypes.pricing

import spock.lang.Specification

class NumericRangeSpec extends Specification {

    def "supports big decimal values"() {
        given:
        CalculatorRange range = CalculatorRange.numeric(
                new BigDecimal("0"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )

        expect:
        range.supports(new BigDecimal("5"))
        !(range.supports(5))
        !(range.supports("5"))
        !(range.supports(5.0d))
    }

    def "contains value in range"() {
        given:
        CalculatorRange range = CalculatorRange.numeric(
                new BigDecimal("10"),
                new BigDecimal("20"),
                CalculatorId.generate()
        )

        expect:
        range.contains(new BigDecimal("10"))
        range.contains(new BigDecimal("15"))
        !(range.contains(new BigDecimal("20")))
        !(range.contains(new BigDecimal("5")))
        !(range.contains(new BigDecimal("25")))
    }

    def "does not contain value of wrong type"() {
        given:
        CalculatorRange range = CalculatorRange.numeric(
                new BigDecimal("0"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )

        expect:
        !(range.contains("5"))
        !(range.contains(5))
    }

    def "throws when min greater than max"() {
        when:
        CalculatorRange.numeric(
                new BigDecimal("20"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "throws when min equals max"() {
        when:
        CalculatorRange.numeric(
                new BigDecimal("10"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "detects overlap when ranges overlap"() {
        given:
        CalculatorRange range1 = CalculatorRange.numeric(
                new BigDecimal("0"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )
        CalculatorRange range2 = CalculatorRange.numeric(
                new BigDecimal("5"),
                new BigDecimal("15"),
                CalculatorId.generate()
        )

        expect:
        range1.overlaps(range2)
        range2.overlaps(range1)
    }

    def "does not detect overlap when ranges adjacent"() {
        given:
        CalculatorRange range1 = CalculatorRange.numeric(
                new BigDecimal("0"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )
        CalculatorRange range2 = CalculatorRange.numeric(
                new BigDecimal("10"),
                new BigDecimal("20"),
                CalculatorId.generate()
        )

        expect:
        !(range1.overlaps(range2))
        !(range2.overlaps(range1))
    }

    def "detects overlap when one range contains another"() {
        given:
        CalculatorRange larger = CalculatorRange.numeric(
                new BigDecimal("0"),
                new BigDecimal("100"),
                CalculatorId.generate()
        )
        CalculatorRange smaller = CalculatorRange.numeric(
                new BigDecimal("20"),
                new BigDecimal("30"),
                CalculatorId.generate()
        )

        expect:
        larger.overlaps(smaller)
        smaller.overlaps(larger)
    }

    def "is compatible with other numeric ranges"() {
        given:
        CalculatorRange range1 = CalculatorRange.numeric(
                new BigDecimal("0"),
                new BigDecimal("10"),
                CalculatorId.generate()
        )
        CalculatorRange range2 = CalculatorRange.numeric(
                new BigDecimal("20"),
                new BigDecimal("30"),
                CalculatorId.generate()
        )

        expect:
        range1.isCompatibleWith(range2)
    }
}

package com.softwarearchetypes.quantity.money

import spock.lang.Specification

class PercentageSpec extends Specification {

    def "creates percentage from #kind"() {
        when:
        def percentage = factory()

        then:
        percentage.value() == new BigDecimal(expected)

        where:
        kind         | factory                                   | expected
        "int"        | { Percentage.of(50) }                     | "50.00000"
        "BigDecimal" | { Percentage.of(new BigDecimal("25.5")) } | "25.50000"
    }

    def "creates zero percentage"() {
        expect:
        Percentage.zero().value() == BigDecimal.ZERO
    }

    def "rejects a negative percentage"() {
        when:
        Percentage.of(new BigDecimal("-10"))

        then:
        thrown(IllegalArgumentException)
    }

    def "#operation percentages"() {
        expect:
        result.value() == new BigDecimal(expected)

        where:
        operation    | result                                        | expected
        "adds"       | Percentage.of(30).add(Percentage.of(20))      | "50.00000"
        "subtracts"  | Percentage.of(50).subtract(Percentage.of(20)) | "30.00000"
        "multiplies" | Percentage.of(50).multiply(Percentage.of(20)) | "10.00000"
    }

    def "multiplies decimal percentages"() {
        when:
        def result = Percentage.of(new BigDecimal("33.33")).multiply(Percentage.of(new BigDecimal("50")))

        then:
        result.value().compareTo(new BigDecimal("16.66500")) == 0
    }

    def "formats #description percentage"() {
        expect:
        Percentage.of(new BigDecimal(value)).toString() == expected

        where:
        description  | value    | expected
        "decimal"    | "25.5"   | "25.5%"
        "whole"      | "50"     | "50%"
        "zero"       | "0"      | "0%"
        "very small" | "0.01"   | "0.01%"
        "very large" | "999.99" | "999.99%"
    }

    def "subtracts to zero"() {
        expect:
        Percentage.of(10).subtract(Percentage.of(10)).value().compareTo(BigDecimal.ZERO) == 0
    }

    def "rejects subtraction resulting in a negative percentage"() {
        when:
        Percentage.of(10).subtract(Percentage.of(20))

        then:
        thrown(IllegalArgumentException)
    }

    def "creates percentage from fraction"() {
        expect:
        Percentage.ofFraction(fraction).value() == new BigDecimal(expected)

        where:
        fraction | expected
        0.0d     | "0.00000"
        0.25d    | "25.00000"
        0.5d     | "50.00000"
        1.0d     | "100.00000"
        1.5d     | "150.00000"
    }

    def "rounds percentage to five decimal places"() {
        expect:
        Percentage.of(new BigDecimal(value)).value() == new BigDecimal(expected)

        where:
        value      | expected
        "1.123454" | "1.12345"
        "1.123455" | "1.12346"
        "0.000004" | "0.00000"
        "0.000005" | "0.00001"
    }

    def "adding percentages may exceed one hundred percent"() {
        expect:
        Percentage.of(75).add(Percentage.of(50)).value() == new BigDecimal("125.00000")
    }

    def "multiplying by zero returns zero"() {
        expect:
        Percentage.of(50).multiply(Percentage.zero()) == Percentage.zero()
    }

    def "multiplying by one hundred preserves percentage"() {
        given:
        def percentage = Percentage.of(new BigDecimal("33.33"))

        expect:
        percentage.multiply(Percentage.of(100)) == percentage
    }

    def "formats percentage rounded to two decimal places"() {
        expect:
        Percentage.of(new BigDecimal(value)).toString() == expected

        where:
        value    | expected
        "25.554" | "25.55%"
        "25.555" | "25.56%"
        "0.004"  | "0%"
        "0.005"  | "0.01%"
    }

    def "rejects negative percentage after rounding"() {
        when:
        Percentage.of(new BigDecimal("-0.00001"))

        then:
        thrown(IllegalArgumentException)
    }
}

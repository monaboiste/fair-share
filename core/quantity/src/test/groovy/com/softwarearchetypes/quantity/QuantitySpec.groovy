package com.softwarearchetypes.quantity

import spock.lang.Specification

class QuantitySpec extends Specification {

    def "creates quantity from #kind amount"() {
        when:
        def quantity = factory()

        then:
        quantity.amount() == expected
        quantity.unit() == Unit.kilograms()

        where:
        kind         | factory                                                    | expected
        "BigDecimal" | { Quantity.of(new BigDecimal("100.5"), Unit.kilograms()) } | new BigDecimal("100.5")
        "double"     | { Quantity.of(50.75d, Unit.kilograms()) }                  | new BigDecimal("50.75")
        "int"        | { Quantity.of(1000, Unit.kilograms()) }                    | new BigDecimal("1000")
    }

    def "rejects invalid quantity"() {
        when:
        Quantity.of(amount, unit)

        then:
        thrown(IllegalArgumentException)

        where:
        amount                | unit
        (BigDecimal) null     | Unit.kilograms()
        new BigDecimal("100") | null
        new BigDecimal("-10") | Unit.kilograms()
    }

    def "allows zero amount"() {
        expect:
        Quantity.of(BigDecimal.ZERO, Unit.pieces()).amount() == BigDecimal.ZERO
    }

    def "adds quantities with #description amounts"() {
        when:
        def result = Quantity.of(first, unit).add(Quantity.of(second, unit))

        then:
        result.amount() == expected
        result.unit() == unit

        where:
        description | first | second | unit             | expected
        "whole"     | 100   | 50     | Unit.kilograms() | new BigDecimal("150")
        "decimal"   | 10.5d | 5.25d  | Unit.liters()    | new BigDecimal("15.75")
    }

    def "rejects adding quantities with different units"() {
        when:
        Quantity.of(100, Unit.kilograms()).add(Quantity.of(50, Unit.liters()))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("different units")
    }

    def "subtracts quantities with #description amounts"() {
        when:
        def result = Quantity.of(first, unit).subtract(Quantity.of(second, unit))

        then:
        result.amount() == expected
        result.unit() == unit

        where:
        description | first  | second | unit             | expected
        "whole"     | 100    | 30     | Unit.kilograms() | new BigDecimal("70")
        "decimal"   | 50.75d | 20.5d  | Unit.meters()    | new BigDecimal("30.25")
    }

    def "rejects subtracting quantities with different units"() {
        when:
        Quantity.of(100, Unit.meters()).subtract(Quantity.of(5, Unit.hours()))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("different units")
    }

    def "rejects subtraction resulting in a negative quantity"() {
        when:
        Quantity.of(50, Unit.pieces()).subtract(Quantity.of(100, Unit.pieces()))

        then:
        thrown(IllegalArgumentException)
    }

    def "quantities use value equality"() {
        given:
        def quantity = Quantity.of(100, Unit.kilograms())

        expect:
        quantity.equals(Quantity.of(100, Unit.kilograms()))
        quantity.hashCode() == Quantity.of(100, Unit.kilograms()).hashCode()
        !quantity.equals(Quantity.of(50, Unit.kilograms()))
        !quantity.equals(Quantity.of(100, Unit.liters()))
        !quantity.equals(null)
        quantity.is(quantity)
    }

    def "renders #amount #unit as #expected"() {
        expect:
        Quantity.of(amount, unit).toString() == expected

        where:
        amount | unit                | expected
        100.5d | Unit.kilograms()    | "100.5 kg"
        25.5d  | Unit.squareMeters() | "25.5 m²"
    }

    def "adding and subtracting zero preserves the quantity value"() {
        given:
        def quantity = Quantity.of(100, Unit.kilograms())
        def zero = Quantity.of(0, Unit.kilograms())

        expect:
        quantity.add(zero) == quantity
        quantity.subtract(zero) == quantity
    }

    def "preserves #description amount"() {
        when:
        def quantity = Quantity.of(amount, Unit.pieces())

        then:
        quantity.amount() == amount

        where:
        description     | amount
        "large"         | new BigDecimal("9999999999999.99")
        "small decimal" | new BigDecimal("0.000001")
    }

    def "preserves precision in arithmetic"() {
        given:
        def first = Quantity.of(new BigDecimal("10.123456789"), Unit.meters())
        def second = Quantity.of(new BigDecimal("5.987654321"), Unit.meters())

        expect:
        first.add(second).amount() == new BigDecimal("16.111111110")
        first.subtract(second).amount() == (new BigDecimal("4.135802468"))
    }

    def "works with every predefined unit"() {
        expect:
        Quantity.of(amount, unit)

        where:
        amount | unit
        100    | Unit.pieces()
        50.5d  | Unit.kilograms()
        25.75d | Unit.liters()
        10     | Unit.meters()
        100    | Unit.squareMeters()
        5      | Unit.cubicMeters()
        8      | Unit.hours()
        30     | Unit.minutes()
    }

    def "works with custom units"() {
        given:
        def unit = Unit.of("widget", "widgets")

        when:
        def quantity = Quantity.of(42, unit)

        then:
        quantity.toString() == "42 widget"
        quantity.amount() == new BigDecimal("42")
        quantity.unit() == unit
    }

    def "arithmetic leaves the original quantity unchanged"() {
        given:
        def original = Quantity.of(100, Unit.kilograms())

        when:
        def result = original.add(Quantity.of(50, Unit.kilograms()))

        then:
        original.amount() == new BigDecimal("100")
        result.amount() == new BigDecimal("150")
        !original.is(result)
    }

    def "compares quantities with the same unit"() {
        given:
        def smaller = Quantity.of(50, Unit.kilograms())
        def larger = Quantity.of(100, Unit.kilograms())

        expect:
        smaller < larger
        larger > smaller
        smaller <=> Quantity.of(50, Unit.kilograms()) == 0
    }

    def "rejects comparing quantities with different units"() {
        when:
        Quantity.of(100, Unit.kilograms()) <=> Quantity.of(50, Unit.liters())

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("different units")
    }

    def "rejects comparing to null"() {
        when:
        Quantity.of(100, Unit.kilograms()).compareTo(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "equal quantities ignore BigDecimal scale"() {
        expect:
        Quantity.of(new BigDecimal("1"), Unit.kilograms()) ==
                Quantity.of(new BigDecimal("1.00"), Unit.kilograms())
    }
}

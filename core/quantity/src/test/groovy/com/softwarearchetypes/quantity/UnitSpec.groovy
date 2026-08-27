package com.softwarearchetypes.quantity

import spock.lang.Specification

class UnitSpec extends Specification {

    def "creates a unit with symbol and name"() {
        when:
        def unit = Unit.of("kg", "kilograms")

        then:
        unit.symbol() == "kg"
        unit.name() == "kilograms"
    }

    def "rejects an invalid unit"() {
        when:
        Unit.of(symbol, name)

        then:
        thrown(IllegalArgumentException)

        where:
        symbol | name
        null   | "kilograms"
        ""     | "kilograms"
        "   "  | "kilograms"
        "kg"   | null
        "kg"   | ""
        "kg"   | "   "
    }

    def "creates predefined #name unit"() {
        expect:
        unit.symbol() == symbol
        unit.name() == name

        where:
        unit                | symbol | name
        Unit.pieces()       | "pcs"  | "pieces"
        Unit.kilograms()    | "kg"   | "kilograms"
        Unit.liters()       | "l"    | "liters"
        Unit.meters()       | "m"    | "meters"
        Unit.squareMeters() | "m²"   | "square meters"
        Unit.cubicMeters()  | "m³"   | "cubic meters"
        Unit.hours()        | "h"    | "hours"
        Unit.minutes()      | "min"  | "minutes"
        Unit.packages()     | "pkg"  | "packages"
        Unit.accounts()     | "acc"  | "accounts"
    }

    def "units use value equality"() {
        given:
        def unit = Unit.of("kg", "kilograms")

        expect:
        unit == Unit.of("kg", "kilograms")
        unit.hashCode() == Unit.of("kg", "kilograms").hashCode()
        unit != Unit.of("g", "grams")
        unit != Unit.of("kg", "kilogrammes")
        unit != null
        unit.is(unit)
    }

    def "returns symbol as string for #description unit"() {
        expect:
        unit.toString() == expected

        where:
        description | unit                | expected
        "simple"    | Unit.kilograms()    | "kg"
        "complex"   | Unit.squareMeters() | "m²"
    }

    def "supports unicode symbol #symbol"() {
        expect:
        Unit.of(symbol, name) == new Unit(symbol, name)
        Unit.of(symbol, name).toString() == symbol

        where:
        symbol | name
        "℃"    | "degrees Celsius"
        "Ω"    | "ohm"
        "m²"   | "square meters"
    }
}

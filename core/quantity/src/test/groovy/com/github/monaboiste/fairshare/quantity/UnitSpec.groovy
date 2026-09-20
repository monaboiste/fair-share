package com.github.monaboiste.fairshare.quantity


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
        ""     | "kilograms"
        "   "  | "kilograms"
        "kg"   | ""
        "kg"   | "   "
    }

    def "creates predefined #name unit"() {
        when:
        def unit = factory()

        then:
        unit.symbol() == symbol
        unit.name() == name

        where:
        symbol | name            | factory
        "pcs"  | "pieces"        | Unit.&pieces
        "kg"   | "kilograms"     | Unit.&kilograms
        "l"    | "liters"        | Unit.&liters
        "m"    | "meters"        | Unit.&meters
        "m²"   | "square meters" | Unit.&squareMeters
        "m³"   | "cubic meters"  | Unit.&cubicMeters
        "h"    | "hours"         | Unit.&hours
        "min"  | "minutes"       | Unit.&minutes
        "pkg"  | "packages"      | Unit.&packages
        "acc"  | "accounts"      | Unit.&accounts
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

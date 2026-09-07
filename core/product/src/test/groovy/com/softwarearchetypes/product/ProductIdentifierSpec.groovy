package com.softwarearchetypes.product

import spock.lang.Specification

class ProductIdentifierSpec extends Specification {

    def "text identifier preserves value and reports type"() {
        given:
        ProductIdentifier identifier = ProductIdentifier.of(" SKU-123 ")

        expect:
        identifier.toString() == " SKU-123 "
        identifier.type() == "TEXT"
    }

    def "uuid identifier normalizes value and reports type"() {
        given:
        String value = "123e4567-e89b-12d3-a456-426614174000"

        when:
        ProductIdentifier identifier = UuidProductIdentifier.of(value.toUpperCase())

        then:
        identifier.toString() == value
        identifier.type() == "UUID"
    }

    def "isbn-10 normalizes separators and reports type"() {
        when:
        ProductIdentifier identifier = Isbn10ProductIdentifier.of("0-201-77060-1")

        then:
        identifier.toString() == "0201770601"
        identifier.type() == "ISBN-10"
    }

    def "gtin normalizes separators and reports length type"() {
        expect:
        GtinProductIdentifier.of(value).toString() == normalized
        GtinProductIdentifier.of(value).type() == type

        where:
        value                    | normalized          | type
        "96385074"              | "96385074"          | "GTIN-8"
        "123456789012"       | "123456789012"      | "GTIN-12"
        "4006381333931"         | "4006381333931"    | "GTIN-13"
        "1 23456 78901 231"       | "12345678901231"    | "GTIN-14"
    }

    def "valid isbn-10 values are accepted"() {
        expect:
        Isbn10ProductIdentifier.of(value).toString() == normalized

        where:
        value              | normalized
        "0201770601"       | "0201770601"
        "0-471-95869-7"    | "0471958697"
    }

    def "null text identifier is rejected"() {
        when:
        ProductIdentifier.of(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "invalid identifier values are rejected"() {
        when:
        switch (kind) {
            case "text" -> ProductIdentifier.of(value)
            case "uuid" -> UuidProductIdentifier.of(value)
            case "isbn" -> Isbn10ProductIdentifier.of(value)
            case "gtin" -> GtinProductIdentifier.of(value)
        }

        then:
        thrown(IllegalArgumentException)

        where:
        kind   | value
        "text" | ""
        "text" | "   "
        "uuid" | "not-a-uuid"
        "uuid" | ""
        "isbn" | "0306406151"
        "isbn" | "123456789"
        "isbn" | ""
        "gtin" | "036002291453"
        "gtin" | "1234567"
        "gtin" | ""
    }
}

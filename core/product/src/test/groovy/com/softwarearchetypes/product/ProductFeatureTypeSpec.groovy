package com.softwarearchetypes.product

import java.time.LocalDate
import spock.lang.Specification

class ProductFeatureTypeSpec extends Specification {

    def "should create and validate #name feature"() {
        when:
        ProductFeatureType feature = factory()

        then:
        feature.isValidValue(validValue)
        !feature.isValidValue(invalidValue)
        feature.toString().contains(name)

        where:
        name      | factory                                                                          | validValue                    | invalidValue
        "color"   | { ProductFeatureType.withAllowedValues("color", "blue") }                       | "blue"                        | "red"
        "year"    | { ProductFeatureType.withNumericRange("year", 2020, 2030) }                       | 2025                          | 2031
        "price"   | { ProductFeatureType.withDecimalRange("price", "1.00", "10.00") }              | new BigDecimal("2.00")       | new BigDecimal("11.00")
        "code"    | { ProductFeatureType.withRegex("code", "[A-Z]+") }                              | "ABC"                         | "abc"
        "date"    | { ProductFeatureType.withDateRange("date", "2025-01-01", "2025-12-31") }      | LocalDate.of(2025, 6, 1)      | LocalDate.of(2026, 1, 1)
        "enabled" | { ProductFeatureType.unconstrained("enabled", FeatureValueType.BOOLEAN) }        | true                          | "true"
    }

    def "should reject #scenario value"() {
        given:
        ProductFeatureType feature = ProductFeatureType.withAllowedValues("color", "blue")

        when:
        feature.validateValue(value)

        then:
        thrown(IllegalArgumentException)

        where:
        scenario             | value
        "null"               | null
        "wrongly typed"      | 1
        "constraint-invalid" | "red"
    }

    def "should compare feature types by name"() {
        given:
        ProductFeatureType first = ProductFeatureType.withAllowedValues("color", "blue")
        ProductFeatureType sameName = ProductFeatureType.withAllowedValues("color", "red")
        ProductFeatureType other = ProductFeatureType.withAllowedValues("size", "small")

        expect:
        first == sameName
        first.hashCode() == sameName.hashCode()
        first != other
        !first.equals("color")
    }

    def "should reject missing constructor arguments"() {
        when:
        new ProductFeatureType(name, constraint)

        then:
        thrown(IllegalArgumentException)

        where:
        name    | constraint
        null    | AllowedValuesConstraint.of("blue")
        " "     | AllowedValuesConstraint.of("blue")
        "color" | null
    }
}

package com.softwarearchetypes.product

import java.time.LocalDate
import spock.lang.Specification

class ProductFeatureInstanceSpec extends Specification {

    def "creates instances with of and fromString"() {
        given:
        ProductFeatureType featureType = ProductFeatureType.withNumericRange("age", 1, 100)

        when:
        ProductFeatureInstance direct = ProductFeatureInstance.of(featureType, 42)
        ProductFeatureInstance parsed = ProductFeatureInstance.fromString(featureType, "42")

        then:
        direct.value() == 42
        parsed.value() == 42
        parsed.valueAsString() == "42"
    }

    def "returns each supported value as a string"() {
        expect:
        ProductFeatureInstance.of(featureType, value).valueAsString() == expected

        where:
        featureType                                                           | value                    | expected
        ProductFeatureType.withAllowedValues("color", "red")                  | "red"                    | "red"
        ProductFeatureType.withNumericRange("count", 1, 10)                   | 3                        | "3"
        ProductFeatureType.withDecimalRange("price", "1", "10")               | new BigDecimal("2.50")   | "2.50"
        ProductFeatureType.withDateRange("date", "2025-01-01", "2025-12-31")  | LocalDate.of(2025, 1, 2) | "2025-01-02"
        ProductFeatureType.unconstrained("enabled", FeatureValueType.BOOLEAN) | true                     | "true"
    }

    def "returns typed values through accessors"() {
        given:
        ProductFeatureInstance instance = ProductFeatureInstance.of(featureType, value)

        expect:
        accessor.call(instance) == value

        where:
        featureType                                                           | value                    | accessor
        ProductFeatureType.withAllowedValues("color", "red")                  | "red"                    | { it.asString() }
        ProductFeatureType.withNumericRange("count", 1, 10)                   | 3                        | { it.asInt() }
        ProductFeatureType.withDecimalRange("price", "1", "10")               | new BigDecimal("2.50")   | { it.asDecimal() }
        ProductFeatureType.withDateRange("date", "2025-01-01", "2025-12-31")  | LocalDate.of(2025, 1, 2) | { it.asDate() }
        ProductFeatureType.unconstrained("enabled", FeatureValueType.BOOLEAN) | true                     | { it.asBoolean() }
    }

    def "rejects wrong types through typed accessors"() {
        given:
        ProductFeatureInstance instance = ProductFeatureInstance.of(featureType, value)

        when:
        accessor.call(instance)

        then:
        thrown(IllegalStateException)

        where:
        featureType                                          | value | accessor
        ProductFeatureType.withNumericRange("count", 1, 10)  | 3     | { it.asString() }
        ProductFeatureType.withAllowedValues("color", "red") | "red" | { it.asInt() }
        ProductFeatureType.withAllowedValues("color", "red") | "red" | { it.asDecimal() }
        ProductFeatureType.withAllowedValues("color", "red") | "red" | { it.asDate() }
        ProductFeatureType.withAllowedValues("color", "red") | "red" | { it.asBoolean() }
    }

    def "supports equality type checks and string representation"() {
        given:
        ProductFeatureType color = ProductFeatureType.withAllowedValues("color", "red", "blue")
        ProductFeatureType sameName = ProductFeatureType.withAllowedValues("color", "red", "green")
        ProductFeatureType size = ProductFeatureType.withAllowedValues("size", "large", "small")
        ProductFeatureInstance red = ProductFeatureInstance.of(color, "red")
        ProductFeatureInstance equal = ProductFeatureInstance.of(sameName, "red")

        expect:
        red == equal
        red.hashCode() == equal.hashCode()
        red != ProductFeatureInstance.of(color, "blue")
        red != ProductFeatureInstance.of(size, "large")
        red.isOfType(color)
        red.isOfType(sameName)
        !red.isOfType(size)
        red.toString() == "ProductFeatureInstance{color=red}"
    }
}

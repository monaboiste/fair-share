package com.softwarearchetypes.product

import java.time.LocalDate
import spock.lang.Specification

class FeatureValueConstraintSpec extends Specification {

    static class NumericRangeConstraintSpec extends Specification {

        def "should accept value within range"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100)

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [1, 50, 100]
        }

        def "should reject value outside range"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100)

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << [0, 101, -5]
        }

        def "should reject non integer values"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100)

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << ["50", 50.5, null]
        }

        def "should reject invalid range"() {
            when:
            NumericRangeConstraint.between(100, 1)

            then:
            thrown(IllegalArgumentException)
        }

        def "should allow same min and max"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(42, 42)

            when:
            boolean result = constraint.isValid(value)

            then:
            result == expected

            where:
            value | expected
            42    | true
            41    | false
            43    | false
        }

        def "should have correct value type"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100)

            when:
            FeatureValueType valueType = constraint.valueType()
            String type = constraint.type()

            then:
            FeatureValueType.INTEGER == valueType
            "NUMERIC_RANGE" == type
        }

        def "should convert from string"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100)

            when:
            Object result = constraint.fromString("50")

            then:
            50 == result
        }

        def "should reject invalid value from string"() {
            given:
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100)

            when:
            constraint.fromString("150")

            then:
            thrown(IllegalArgumentException)
        }
    }

    static class DecimalRangeConstraintSpec extends Specification {

        def "should accept value within range"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0")

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [new BigDecimal("0.5"), new BigDecimal("50.25"), new BigDecimal("100.0")]
        }

        def "should reject value outside range"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << [new BigDecimal("0.4"), new BigDecimal("100.1"), new BigDecimal("-1.0")]
        }

        def "should reject non big decimal values"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << ["50.0", 50, 50.0d, null]
        }

        def "should reject invalid range"() {
            when:
            DecimalRangeConstraint.of("100.0", "0.5")

            then:
            thrown(IllegalArgumentException)
        }

        def "should allow same min and max"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("42.5", "42.5")

            when:
            boolean result = constraint.isValid(value)

            then:
            result == expected

            where:
            value                  | expected
            new BigDecimal("42.5") | true
            new BigDecimal("42.4") | false
            new BigDecimal("42.6") | false
        }

        def "should have correct value type"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0")

            when:
            FeatureValueType valueType = constraint.valueType()
            String type = constraint.type()

            then:
            FeatureValueType.DECIMAL == valueType
            "DECIMAL_RANGE" == type
        }

        def "should convert from string"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0")

            when:
            Object result = constraint.fromString("50.25")

            then:
            new BigDecimal("50.25") == result
        }

        def "should reject invalid value from string"() {
            given:
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0")

            when:
            constraint.fromString("150.0")

            then:
            thrown(IllegalArgumentException)
        }
    }

    static class DateRangeConstraintSpec extends Specification {

        def "should accept date within range"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-01-01", "2024-12-31")

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 15), LocalDate.of(2024, 12, 31)]
        }

        def "should reject date outside range"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-01-01", "2024-12-31")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << [LocalDate.of(2023, 12, 31), LocalDate.of(2025, 1, 1)]
        }

        def "should reject non date values"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-01-01", "2024-12-31")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << ["2024-06-15", 20240615, null]
        }

        def "should reject invalid range"() {
            when:
            DateRangeConstraint.between("2024-12-31", "2024-01-01")

            then:
            thrown(IllegalArgumentException)
        }

        def "should allow same from and to"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-06-15", "2024-06-15")

            when:
            boolean result = constraint.isValid(value)

            then:
            result == expected

            where:
            value                     | expected
            LocalDate.of(2024, 6, 15) | true
            LocalDate.of(2024, 6, 14) | false
            LocalDate.of(2024, 6, 16) | false
        }

        def "should have correct value type"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-01-01", "2024-12-31")

            when:
            FeatureValueType valueType = constraint.valueType()
            String type = constraint.type()

            then:
            FeatureValueType.DATE == valueType
            "DATE_RANGE" == type
        }

        def "should convert from string"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-01-01", "2024-12-31")

            when:
            Object result = constraint.fromString("2024-06-15")

            then:
            LocalDate.of(2024, 6, 15) == result
        }

        def "should reject invalid value from string"() {
            given:
            FeatureValueConstraint constraint = DateRangeConstraint.between("2024-01-01", "2024-12-31")

            when:
            constraint.fromString("2025-06-15")

            then:
            thrown(IllegalArgumentException)
        }
    }

    static class RegexConstraintSpec extends Specification {

        def "should accept matching value"() {
            given:
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}\$")

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << ["AB-1234", "XY-9999", "PL-0001"]
        }

        def "should reject non matching value"() {
            given:
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}\$")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << ["ab-1234", "ABC-1234", "AB-123", "AB1234"]
        }

        def "should reject non string values"() {
            given:
            FeatureValueConstraint constraint = RegexConstraint.of("^\\d+\$")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << [123, null]
        }

        def "should reject blank pattern"() {
            when:
            RegexConstraint.of(pattern)

            then:
            thrown(IllegalArgumentException)

            where:
            pattern << ["", "   ", null]
        }

        def "should have correct value type"() {
            given:
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]+\$")

            when:
            FeatureValueType valueType = constraint.valueType()
            String type = constraint.type()

            then:
            FeatureValueType.TEXT == valueType
            "REGEX" == type
        }

        def "should convert from string"() {
            given:
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}\$")

            when:
            Object result = constraint.fromString("AB-1234")

            then:
            "AB-1234" == result
        }

        def "should reject invalid value from string"() {
            given:
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}\$")

            when:
            constraint.fromString("invalid")

            then:
            thrown(IllegalArgumentException)
        }
    }

    static class AllowedValuesConstraintSpec extends Specification {

        def "should accept allowed value"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green")

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << ["red", "blue", "green"]
        }

        def "should reject not allowed value"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << ["yellow", "RED", ""]
        }

        def "should reject non string values"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("1", "2", "3")

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            value << [1, null]
        }

        def "should reject empty allowed values"() {
            when:
            AllowedValuesConstraint.of()

            then:
            thrown(IllegalArgumentException)
        }

        def "should work with single allowed value"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("only")

            when:
            boolean result = constraint.isValid(value)

            then:
            result == expected

            where:
            value   | expected
            "only"  | true
            "other" | false
        }

        def "should have correct value type"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("a", "b", "c")

            when:
            FeatureValueType valueType = constraint.valueType()
            String type = constraint.type()

            then:
            FeatureValueType.TEXT == valueType
            "ALLOWED_VALUES" == type
        }

        def "should convert from string"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green")

            when:
            Object result = constraint.fromString("red")

            then:
            "red" == result
        }

        def "should reject invalid value from string"() {
            given:
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green")

            when:
            constraint.fromString("yellow")

            then:
            thrown(IllegalArgumentException)
        }
    }

    static class UnconstrainedSpec extends Specification {

        def "should accept any text value"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.TEXT)

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << ["anything", "", "special chars: !@#\$%"]
        }

        def "should accept any integer value"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.INTEGER)

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [0, -100, Integer.MAX_VALUE]
        }

        def "should accept any decimal value"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.DECIMAL)

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [new BigDecimal("0"), new BigDecimal("-100.5"), new BigDecimal("999999.999")]
        }

        def "should accept any date value"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.DATE)

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31), LocalDate.now()]
        }

        def "should accept any boolean value"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.BOOLEAN)

            when:
            boolean result = constraint.isValid(value)

            then:
            result

            where:
            value << [true, false]
        }

        def "should reject wrong type"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(valueType)

            when:
            boolean result = constraint.isValid(value)

            then:
            !result

            where:
            valueType                | value
            FeatureValueType.TEXT    | 123
            FeatureValueType.INTEGER | "123"
            FeatureValueType.TEXT    | null
        }

        def "should reject null value type"() {
            when:
            new Unconstrained(null)

            then:
            thrown(IllegalArgumentException)
        }

        def "should have correct type identifier"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.TEXT)

            when:
            String result = constraint.type()

            then:
            "UNCONSTRAINED" == result
        }

        def "should return correct value type"() {
            given:
            FeatureValueConstraint constraint = new Unconstrained(valueType)

            when:
            FeatureValueType result = constraint.valueType()

            then:
            valueType == result

            where:
            valueType << [FeatureValueType.TEXT, FeatureValueType.INTEGER]
        }
    }

    static class FeatureValueTypeSpec extends Specification {

        def "should cast text from string"() {
            when:
            Object result = FeatureValueType.TEXT.castFrom("hello")

            then:
            "hello" == result
        }

        def "should cast integer from string"() {
            when:
            Object result = FeatureValueType.INTEGER.castFrom("42")

            then:
            42 == result
        }

        def "should cast decimal from string"() {
            when:
            Object result = FeatureValueType.DECIMAL.castFrom("42.5")

            then:
            new BigDecimal("42.5") == result
        }

        def "should cast date from string"() {
            when:
            Object result = FeatureValueType.DATE.castFrom("2024-06-15")

            then:
            LocalDate.of(2024, 6, 15) == result
        }

        def "should cast boolean from string"() {
            when:
            Object result = FeatureValueType.BOOLEAN.castFrom(value)

            then:
            expected == result

            where:
            value   | expected
            "true"  | true
            "false" | false
        }

        def "should cast text to string"() {
            when:
            String result = FeatureValueType.TEXT.castTo("hello")

            then:
            "hello" == result
        }

        def "should cast integer to string"() {
            when:
            String result = FeatureValueType.INTEGER.castTo(42)

            then:
            "42" == result
        }

        def "should cast decimal to string"() {
            when:
            String result = FeatureValueType.DECIMAL.castTo(new BigDecimal("42.5"))

            then:
            "42.5" == result
        }

        def "should cast date to string"() {
            when:
            String result = FeatureValueType.DATE.castTo(LocalDate.of(2024, 6, 15))

            then:
            "2024-06-15" == result
        }

        def "should cast boolean to string"() {
            when:
            String result = FeatureValueType.BOOLEAN.castTo(value)

            then:
            expected == result

            where:
            value | expected
            true  | "true"
            false | "false"
        }

        def "should check instance correctly"() {
            when:
            boolean result = valueType.isInstance(value)

            then:
            result == expected

            where:
            valueType                | value                  | expected
            FeatureValueType.TEXT    | "hello"                | true
            FeatureValueType.INTEGER | 42                     | true
            FeatureValueType.DECIMAL | new BigDecimal("42.5") | true
            FeatureValueType.DATE    | LocalDate.now()        | true
            FeatureValueType.BOOLEAN | true                   | true
            FeatureValueType.TEXT    | 42                     | false
            FeatureValueType.INTEGER | "42"                   | false
        }
    }
}

package com.softwarearchetypes.pricing

import java.math.BigDecimal
import spock.lang.Specification



class NumericRangeSpec extends Specification {
    def "should support big decimal values"() {        given:
        NumericRange range = CalculatorRange.numeric(
            new BigDecimal("0"),
            new BigDecimal("10"),
            CalculatorId.generate()
        )
        and:
        assert range.supports(new BigDecimal("5"))
        assert !(range.supports(5))
        assert !(range.supports("5"))
        assert !(range.supports(5.0d))
    }
    def "should contain value in range"() {        given:
        NumericRange range = CalculatorRange.numeric(
            new BigDecimal("10"),
            new BigDecimal("20"),
            CalculatorId.generate()
        )
        and:
        assert range.contains(new BigDecimal("10"))
        assert range.contains(new BigDecimal("15"))
        assert !(range.contains(new BigDecimal("20")))
        assert !(range.contains(new BigDecimal("5")))
        assert !(range.contains(new BigDecimal("25")))
    }
    def "should not contain value of wrong type"() {        given:
        NumericRange range = CalculatorRange.numeric(
            new BigDecimal("0"),
            new BigDecimal("10"),
            CalculatorId.generate()
        )
        and:
        assert !(range.contains("5"))
        assert !(range.contains(5))
    }
    def "should throw when min greater than max"() {        given:
        shouldFail(IllegalArgumentException) {
            CalculatorRange.numeric(
                new BigDecimal("20"),
                new BigDecimal("10"),
                CalculatorId.generate()
            ) }
    }
    def "should throw when min equals max"() {        given:
        shouldFail(IllegalArgumentException) {
            CalculatorRange.numeric(
                new BigDecimal("10"),
                new BigDecimal("10"),
                CalculatorId.generate()
            ) }
    }
    def "should detect overlap when ranges overlap"() {        given:
        NumericRange range1 = CalculatorRange.numeric(
            new BigDecimal("0"),
            new BigDecimal("10"),
            CalculatorId.generate()
        )
        NumericRange range2 = CalculatorRange.numeric(
            new BigDecimal("5"),
            new BigDecimal("15"),
            CalculatorId.generate()
        )
        and:
        assert range1.overlaps(range2)
        assert range2.overlaps(range1)
    }
    def "should not detect overlap when ranges adjacent"() {        given:
        NumericRange range1 = CalculatorRange.numeric(
            new BigDecimal("0"),
            new BigDecimal("10"),
            CalculatorId.generate()
        )
        NumericRange range2 = CalculatorRange.numeric(
            new BigDecimal("10"),
            new BigDecimal("20"),
            CalculatorId.generate()
        )
        and:
        assert !(range1.overlaps(range2))
        assert !(range2.overlaps(range1))
    }
    def "should detect overlap when one range contains another"() {        given:
        NumericRange larger = CalculatorRange.numeric(
            new BigDecimal("0"),
            new BigDecimal("100"),
            CalculatorId.generate()
        )
        NumericRange smaller = CalculatorRange.numeric(
            new BigDecimal("20"),
            new BigDecimal("30"),
            CalculatorId.generate()
        )
        and:
        assert larger.overlaps(smaller)
        assert smaller.overlaps(larger)
    }
    def "should be compatible with other numeric ranges"() {        given:
        NumericRange range1 = CalculatorRange.numeric(
            new BigDecimal("0"),
            new BigDecimal("10"),
            CalculatorId.generate()
        )
        NumericRange range2 = CalculatorRange.numeric(
            new BigDecimal("20"),
            new BigDecimal("30"),
            CalculatorId.generate()
        )
        and:
        assert range1.isCompatibleWith(range2)
    }

    private static Throwable shouldFail(Class<? extends Throwable> type, Closure action) {
        try {
            action.call()
        } catch (Throwable exception) {
            assert type.isInstance(exception)
            return exception
        }
        throw new AssertionError("Expected " + type.simpleName)
    }
}

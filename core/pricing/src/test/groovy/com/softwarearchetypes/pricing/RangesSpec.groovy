package com.softwarearchetypes.pricing

import java.math.BigDecimal
import java.time.LocalTime
import java.util.List
import java.util.Map
import spock.lang.Specification



class RangesSpec extends Specification {
    def "should create ranges with valid non overlapping ranges"() {        given:
        List<CalculatorRange> rangesList = List.of(
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
            CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), CalculatorId.generate()),
            CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), CalculatorId.generate())
        )
        and:
        Ranges ranges = new Ranges("quantity", rangesList)
        and:
        assert ranges.size() == 3
    }
    def "should throw when ranges are empty"() {        given:
        shouldFail(IllegalArgumentException) {
            new Ranges("quantity", List.of()) }
    }
    def "should throw when range selector is null"() {        given:
        List<CalculatorRange> rangesList = List.of(
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate())
        )
        and:
        shouldFail(IllegalArgumentException) {
            new Ranges(null, rangesList) }
    }
    def "should throw when range selector is blank"() {        given:
        List<CalculatorRange> rangesList = List.of(
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate())
        )
        and:
        shouldFail(IllegalArgumentException) {
            new Ranges("  ", rangesList) }
    }
    def "should throw when ranges overlap"() {        given:
        List<CalculatorRange> rangesList = List.of(
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
            CalculatorRange.numeric(new BigDecimal("5"), new BigDecimal("15"), CalculatorId.generate())
        )
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { new Ranges("quantity", rangesList) }

        assert exception.getMessage().contains("overlap")
    }
    def "should throw when ranges have incompatible types"() {        given:
        List<CalculatorRange> rangesList = List.of(
            CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
            CalculatorRange.time(LocalTime.of(8, 0), LocalTime.of(18, 0), CalculatorId.generate())
        )
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { new Ranges("param", rangesList) }

        assert exception.getMessage().contains("same type")
    }
    def "should find matching range for numeric value"() {        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
            "quantity",
            List.of(
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), matchingRangeCalculatorId),
                CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("100"), CalculatorId.generate())
            )
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("25")))
        and:
        CalculatorRange matchingRange = ranges.findMatching(params).orElseThrow()
        and:
        assert matchingRange.calculatorId() == matchingRangeCalculatorId
    }
    def "should find matching range for time value"() {        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
            "time",
            List.of(
                CalculatorRange.time(LocalTime.of(8, 0), LocalTime.of(18, 0), matchingRangeCalculatorId),
                CalculatorRange.time(LocalTime.of(18, 0), LocalTime.of(8, 0), CalculatorId.generate())
            )
        )

        Parameters params = new Parameters(Map.of("time", LocalTime.of(15, 30)))
        and:
        CalculatorRange matchingRange = ranges.findMatching(params).orElseThrow()
        and:
        assert matchingRange.calculatorId() == matchingRangeCalculatorId
    }
    def "should return empty when no matching range"() {        given:
        Ranges ranges = new Ranges(
            "quantity",
            List.of(
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), CalculatorId.generate())
            )
        )

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5")))
        and:
        var result = ranges.findMatching(params)
        and:
        assert result.isEmpty()
    }
    def "should throw when parameter not found"() {        given:
        Ranges ranges = new Ranges(
            "quantity",
            List.of(
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate())
            )
        )

        Parameters params = new Parameters(Map.of("weight", new BigDecimal("5")))
        and:
        IllegalArgumentException exception = shouldFail(IllegalArgumentException) { ranges.findMatching(params) }

        assert exception.getMessage().contains("quantity")
        assert exception.getMessage().contains("required")
    }
    def "should find first matching range when adjacent"() {        given:
        CalculatorId matchingRangeCalculatorId = CalculatorId.generate()

        Ranges ranges = new Ranges(
            "quantity",
            List.of(
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), CalculatorId.generate()),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("20"), matchingRangeCalculatorId)
            )
        )

        Parameters paramsAt10 = new Parameters(Map.of("quantity", new BigDecimal("10")))
        and:
        CalculatorRange matchingRange = ranges.findMatching(paramsAt10).orElseThrow()
        assert matchingRange.calculatorId() == matchingRangeCalculatorId
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

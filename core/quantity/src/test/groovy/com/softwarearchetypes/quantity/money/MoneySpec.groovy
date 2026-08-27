package com.softwarearchetypes.quantity.money

import spock.lang.Unroll

import javax.money.MonetaryException
import java.math.RoundingMode

import spock.lang.Specification

class MoneySpec extends Specification {

    def "creates PLN money from #kind amount"() {
        when:
        def money = Money.of(amount, "PLN")

        then:
        money.value() == expected

        where:
        kind         | amount                  | expected
        "integer"    | 100                     | new BigDecimal("100")
        "BigDecimal" | new BigDecimal("99.99") | new BigDecimal("99.99")
        "Number"     | (Number) 50.5d          | new BigDecimal("50.5")
    }

    def "#operation two money amounts"() {
        expect:
        result == expected

        where:
        operation   | result                                             | expected
        "adds"      | Money.of(100, "PLN").add(Money.of(50, "PLN"))      | Money.of(150, "PLN")
        "subtracts" | Money.of(100, "PLN").subtract(Money.of(30, "PLN")) | Money.of(70, "PLN")
    }

    def "negates money"() {
        when:
        def result = Money.of(50, "PLN").negate()

        then:
        result == Money.of(-50, "PLN")
        result.isNegative()
    }

    def "returns absolute value using #description method"() {
        when:
        def result = operation(Money.of(amount, "PLN"))

        then:
        result.value().equals(new BigDecimal(expected))
        !result.isNegative()

        where:
        description | amount | expected | operation
        "instance"  | -100   | "100"    | { Money value -> value.abs() }
        "static"    | -75    | "75"     | { Money value -> Money.abs(value) }
    }

    def "divides and returns quotient and remainder"() {
        when:
        def result = Money.of(100, "PLN").divideAndRemainder(new BigDecimal("3"))

        then:
        result.length == 2
        result[0].value().equals(new BigDecimal("33"))
        result[1].value() == BigDecimal.ONE
    }

    def "reports zero state for #amount"() {
        expect:
        Money.of(amount, "PLN").isZero() == expected

        where:
        amount | expected
        0      | true
        1      | false
    }

    def "reports negative state for #amount"() {
        expect:
        Money.of(amount, "PLN").isNegative() == expected

        where:
        amount | expected
        -10    | true
        10     | false
    }

    def "compares greater amounts"() {
        given:
        def lesser = Money.of(50, "PLN")
        def greater = Money.of(100, "PLN")

        expect:
        greater.isGreaterThan(lesser)
        !lesser.isGreaterThan(greater)
        greater.isGreaterThanOrEqualTo(greater)
        Money.of(150, "PLN").isGreaterThanOrEqualTo(greater)
    }

    def "returns the minimum of two money amounts"() {
        expect:
        Money.min(Money.of(100, "PLN"), Money.of(50, "PLN")) == Money.of(50, "PLN")
    }

    def "returns the minimum from a set"() {
        given:
        def amounts = Set.of(Money.of(100, "PLN"), Money.of(25, "PLN"), Money.of(50, "PLN"), Money.of(75, "PLN"))

        when:
        def result = Money.min(amounts)

        then:
        result.present
        result.get().value().equals(new BigDecimal("25"))
    }

    def "returns empty minimum for an empty set"() {
        expect:
        Money.min(Set.of()).empty
    }

    def "returns the maximum of two money amounts"() {
        expect:
        Money.max(Money.of(100, "PLN"), Money.of(50, "PLN")) == Money.of(100, "PLN")
    }

    def "compares money amounts"() {
        given:
        def smaller = Money.of(50, "PLN")
        def larger = Money.of(100, "PLN")

        expect:
        smaller < larger
        larger > smaller
        smaller == Money.of(50, "PLN")
    }

    def "money uses value equality"() {
        given:
        def money = Money.of(100, "PLN")

        expect:
        money == Money.of(100, "PLN")
        money.hashCode() == Money.of(100, "PLN").hashCode()
        money != Money.of(50, "PLN")
        money != null
        money.is(money)
    }

    def "renders money"() {
        expect:
        Money.of(new BigDecimal("123.45"), "PLN").toString() == "PLN 123.45"
    }

    def "returns value as BigDecimal"() {
        given:
        def value = new BigDecimal("99.99")

        expect:
        Money.of(value, "PLN").value() == value
    }

    def "arithmetic with zero preserves money value"() {
        given:
        def money = Money.of(100, "PLN")
        def zero = Money.zero("PLN")

        expect:
        money.add(zero) == money
        money.subtract(zero) == money
    }

    def "handles negative amounts in comparisons"() {
        given:
        def negative = Money.of(-50, "PLN")
        def positive = Money.of(50, "PLN")

        expect:
        negative.isNegative()
        !positive.isNegative()
        positive.isGreaterThan(negative)
        !negative.isGreaterThan(positive)
    }

    def "multiplies money by #percentage percent"() {
        when:
        def result = Money.of(amount, "PLN").multiply(Percentage.of(new BigDecimal(percentage)))

        then:
        result.value() == new BigDecimal(expected)

        where:
        amount | percentage | expected
        1000   | "20"       | "200.00"
        200    | "50"       | "100.00"
        150    | "100"      | "150.00"
        500    | "0"        | "0"
        1000   | "12.5"     | "125.00"
        10000  | "0.5"      | "50.00"
        100    | "150"      | "150.00"
        100    | "33.33"    | "33.33"
    }

    def "multiplying by percentage preserves currency"() {
        when:
        def result = Money.of(1000, "EUR").multiply(Percentage.of(25))

        then:
        result.value() == new BigDecimal("250.00")
        result.currency() == "EUR"
    }

    def "divides money with default rounding"() {
        when:
        def result = Money.of(100, "PLN").divide(new BigDecimal("3"))

        then:
        result.currency() == "PLN"
        result.value() > new BigDecimal("33")
        result.value() < new BigDecimal("34")
    }

    def "divides money with specified rounding"() {
        when:
        def result = Money.of(105, "PLN").divide(new BigDecimal("15"), RoundingMode.HALF_UP)

        then:
        result.value() == new BigDecimal("7")
        result.currency() == "PLN"
    }

    def "division preserves currency"() {
        when:
        def result = Money.of(150, "EUR").divide(new BigDecimal("10"))

        then:
        result.currency() == "EUR"
        result.value() > new BigDecimal("14")
        result.value() < new BigDecimal("16")
    }

    def "division uses #roundingMode rounding"() {
        expect:
        Money.of(100, "PLN").divide(new BigDecimal("3"), roundingMode).value() == new BigDecimal(expected)

        where:
        roundingMode         | expected
        RoundingMode.UP      | "33.34"
        RoundingMode.DOWN    | "33.33"
        RoundingMode.HALF_UP | "33.33"
    }

    def "returns #currency currency code"() {
        expect:
        money.currency() == currency

        where:
        money                | currency
        Money.of(100, "PLN") | "PLN"
        Money.of(200, "EUR") | "EUR"
        Money.of(300, "USD") | "USD"
        Money.of(400, "GBP") | "GBP"
    }

    def "creates zero money in #currency"() {
        when:
        def money = Money.zero(currency)

        then:
        money.isZero()
        money.currency() == currency
        money.value() == BigDecimal.ZERO

        where:
        currency << ["PLN", "EUR", "USD"]
    }

    def "arithmetic chain preserves currency"() {
        when:
        def result = Money.of(100, "EUR")
                .divide(new BigDecimal("2"))
                .multiply(new BigDecimal("3"))
                .divide(new BigDecimal("5"))

        then:
        result.currency() == "EUR"
        result.value() > new BigDecimal("29")
        result.value() < new BigDecimal("31")
    }

    def "exposes JSR 354 currency unit"() {
        expect:
        Money.of(1, "EUR").currencyUnit().currencyCode == "EUR"
    }

    @Unroll("#description rejects different currencies")
    def "rejects arithmetic across different currencies"() {
        when:
        operation()

        then:
        def exception = thrown(MonetaryException)
        exception.message.contains("Currency mismatch")

        where:
        description             | operation
        "add"                   | { Money.of(100, "PLN").add(Money.of(50, "EUR")) }
        "subtract"              | { Money.of(100, "PLN").subtract(Money.of(50, "EUR")) }
        "compareTo"             | { Money.of(100, "PLN") <=> Money.of(100, "EUR") }
        "isGreaterThan"         | { Money.of(100, "PLN").isGreaterThan(Money.of(50, "EUR")) }
        "min"                   | { Money.min(Money.of(100, "PLN"), Money.of(50, "EUR")) }
        "max"                   | { Money.max(Money.of(100, "PLN"), Money.of(50, "EUR")) }
    }

    def "normalizes monetary value"() {
        expect:
        Money.of(input, "PLN").value() == new BigDecimal(expected)

        where:
        input                            | expected
        new BigDecimal("100.0000000000") | "100"
        new BigDecimal("1.2300000000")   | "1.23"
        new BigDecimal("0.0000000001")   | "0.0000000001"
        new BigDecimal("0.00000000006")  | "0.0000000001"
        new BigDecimal("10000000000")    | "10000000000"
    }

    def "removes floating point artifacts"() {
        expect:
        Money.of(0.1d + 0.2d, "PLN").value() == new BigDecimal("0.3")
    }

    def "multiplies money by a number"() {
        expect:
        Money.of(100, "PLN").multiply(multiplier) == Money.of(expected, "PLN")

        where:
        multiplier | expected
        2          | 200
        0.5d       | 50
        -2         | -200
        0          | 0
    }

    def "zero is not negative"() {
        expect:
        !Money.of(0, "PLN").isNegative()
    }

    def "compareTo returns zero for equal amounts"() {
        expect:
        Money.of(50, "PLN").compareTo(Money.of(50, "PLN")) == 0
    }

    def "minimum and maximum handle equal values"() {
        given:
        def one = Money.of(50, "PLN")
        def two = Money.of(50, "PLN")

        expect:
        Money.min(one, two) == Money.of(50, "PLN")
        Money.max(one, two) == Money.of(50, "PLN")
    }

    def "divide and remainder returns zero remainder for exact division"() {
        when:
        def result = Money.of(100, "PLN").divideAndRemainder(new BigDecimal("10"))

        then:
        result.length == 2
        result[0] == Money.of(10, "PLN")
        result[1] == Money.of(0, "PLN")
    }

    def "cannot divide by zero"() {
        when:
        Money.of(100, "PLN").divide(BigDecimal.ZERO)

        then:
        thrown(ArithmeticException)
    }

    def "cannot divide and remainder by zero"() {
        when:
        Money.of(100, "PLN").divideAndRemainder(BigDecimal.ZERO)

        then:
        thrown(ArithmeticException)
    }

    def "default division returns expected normalized value"() {
        expect:
        Money.of(100, "PLN")
                .divide(new BigDecimal("3"))
                .value() == new BigDecimal("33.3333333333")
    }
}

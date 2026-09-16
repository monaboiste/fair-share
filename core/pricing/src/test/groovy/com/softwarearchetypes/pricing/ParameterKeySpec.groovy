package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class ParameterKeySpec extends Specification {
    def "rejects missing and null values by naming the parameter"() {
        given:
        def input = new ParameterKey<>("quantity", BigDecimal.class)

        expect:
        thrownMessage(input, Parameters.empty()) == "Required parameter 'quantity' is absent"
        thrownMessage(input, new Parameters([quantity: null])) == "Required parameter 'quantity' is absent"
    }

    def "reads convertible Money and BigDecimal values"() {
        expect:
        Parameters.of("amount", "PLN 12.50").get(new ParameterKey<>("amount", Money.class)) == Money.of(new BigDecimal("12.50"), "PLN")
        Parameters.of("quantity", "12.50").get(new ParameterKey<>("quantity", BigDecimal.class)) == new BigDecimal("12.50")
    }

    def "rejects wrong type and invalid format with expected semantic type and cause"() {
        when:
        Parameters.of("amount", 12).get(new ParameterKey<>("amount", Money.class))

        then:
        def wrongType = thrown(IllegalArgumentException)
        wrongType.message == "Parameter 'amount' must be convertible to Money"
        wrongType.cause.message == "Cannot convert 12 to Money"

        when:
        Parameters.of("quantity", "not-a-number").get(new ParameterKey<>("quantity", BigDecimal.class))

        then:
        def invalidFormat = thrown(IllegalArgumentException)
        invalidFormat.message == "Parameter 'quantity' must be convertible to BigDecimal"
        invalidFormat.cause instanceof NumberFormatException
    }

    def "uses strict class casting for other declared types"() {
        given:
        def input = new ParameterKey<>("unit", ChronoUnit)

        expect:
        Parameters.of("unit", ChronoUnit.DAYS).get(input) == ChronoUnit.DAYS

        when:
        Parameters.of("unit", "DAYS").get(input)

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "Parameter 'unit' must be convertible to ChronoUnit"
        error.cause instanceof ClassCastException
    }

    def "typed public overloads preserve values"() {
        given:
        def key = new ParameterKey<String>("name", String.class)
        def parameters = Parameters.of(key, "one").with(key, "two")

        expect:
        parameters.get("name", String.class) == "two"
        Parameters.of(key, "one").get(key) == "one"
        parameters.values().name == "two"
    }

    def "explicit null values remain present"() {
        expect:
        Parameters.empty().with("nullable", null).contains("nullable")
    }

    def "values cannot be mutated"() {
        given:
        def parameters = Parameters.of("name", "value")

        when:
        parameters.values().put("other", "value")

        then:
        thrown(UnsupportedOperationException)
    }

    def "equality and hashing use stable descriptor identity rather than readers"() {
        given:
        def moneyOne = new ParameterKey<>("amount", Money.class)
        def moneyTwo = new ParameterKey<>("amount", Money.class)
        def decimal = new ParameterKey<>("amount", BigDecimal.class)

        expect:
        moneyOne == moneyTwo
        moneyOne.hashCode() == moneyTwo.hashCode()
        moneyOne != decimal
        [moneyOne, moneyTwo] as Set == [moneyOne] as Set
    }

    private static <T> String thrownMessage(ParameterKey<T> input, Parameters parameters) {
        try {
            parameters.get(input)
            return null
        } catch (IllegalArgumentException exception) {
            return exception.message
        }
    }
}

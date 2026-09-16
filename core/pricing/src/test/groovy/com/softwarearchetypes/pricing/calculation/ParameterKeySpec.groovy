package com.softwarearchetypes.pricing.calculation

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

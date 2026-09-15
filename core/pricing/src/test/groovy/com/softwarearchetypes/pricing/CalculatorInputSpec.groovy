package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class CalculatorInputSpec extends Specification {
    def "rejects missing and null values by naming the parameter"() {
        given:
        def input = CalculatorInput.bigDecimal("quantity")

        expect:
        thrownMessage(input, Parameters.empty()) == "Required parameter 'quantity' is absent"
        thrownMessage(input, new Parameters([quantity: null])) == "Required parameter 'quantity' is absent"
    }

    def "reads convertible Money and BigDecimal values"() {
        expect:
        CalculatorInput.money("amount").read(Parameters.of("amount", "PLN 12.50")) == Money.of(new BigDecimal("12.50"), "PLN")
        CalculatorInput.bigDecimal("quantity").read(Parameters.of("quantity", "12.50")) == new BigDecimal("12.50")
    }

    def "rejects wrong type and invalid format with expected semantic type and cause"() {
        when:
        CalculatorInput.money("amount").read(Parameters.of("amount", 12))

        then:
        def wrongType = thrown(IllegalArgumentException)
        wrongType.message == "Parameter 'amount' must be convertible to Money"
        wrongType.cause.message == "Cannot convert 12 to Money"

        when:
        CalculatorInput.bigDecimal("quantity").read(Parameters.of("quantity", "not-a-number"))

        then:
        def invalidFormat = thrown(IllegalArgumentException)
        invalidFormat.message == "Parameter 'quantity' must be convertible to BigDecimal"
        invalidFormat.cause instanceof NumberFormatException
    }

    def "instanceOf accepts only the declared type"() {
        given:
        def input = CalculatorInput.instanceOf("unit", ChronoUnit)

        expect:
        input.read(Parameters.of("unit", ChronoUnit.DAYS)) == ChronoUnit.DAYS

        when:
        input.read(Parameters.of("unit", "DAYS"))

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "Parameter 'unit' must be convertible to ChronoUnit"
        error.cause instanceof ClassCastException
    }

    def "equality and hashing use stable descriptor identity rather than readers"() {
        given:
        def moneyOne = CalculatorInput.money("amount")
        def moneyTwo = CalculatorInput.money("amount")
        def decimal = CalculatorInput.bigDecimal("amount")

        expect:
        moneyOne == moneyTwo
        moneyOne.hashCode() == moneyTwo.hashCode()
        moneyOne != decimal
        [moneyOne, moneyTwo] as Set == [moneyOne] as Set
    }

    private static String thrownMessage(CalculatorInput<?> input, Parameters parameters) {
        try {
            input.read(parameters)
            return null
        } catch (IllegalArgumentException exception) {
            return exception.message
        }
    }
}

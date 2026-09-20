package com.github.monaboiste.fairshare.pricing.calculation


import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class ParameterKeySpec extends Specification {

    def "reads convertible Money and BigDecimal values"() {
        expect:
        Parameters.of("amount", "PLN 12.50").get(new ParameterKey<>("amount", Money.class)) == Money.of(new BigDecimal("12.50"), "PLN")
        Parameters.of("quantity", "12.50").get(new ParameterKey<>("quantity", BigDecimal.class)) == new BigDecimal("12.50")
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
        [moneyOne, moneyTwo, decimal].toSet().size() == 2
    }
}

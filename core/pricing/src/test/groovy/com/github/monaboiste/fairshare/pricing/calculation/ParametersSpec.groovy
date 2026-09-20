package com.github.monaboiste.fairshare.pricing.calculation

import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import spock.lang.Specification

class ParametersSpec extends Specification {

    def "supports parameter construction and immutable updates"() {
        expect:
        parameters.get(expectedKey) == expectedValue

        where:
        parameters                                                          | expectedKey | expectedValue
        Parameters.of("one", 1)                                        | "one"    | 1
        Parameters.of("one", 1, "two", 2)                                   | "two"       | 2
        Parameters.of("one", 1, "two", 2, "three", 3)                       | "three"     | 3
        Parameters.of("one", 1, "two", 2, "three", 3, "four", 4)            | "four"      | 4
        Parameters.of("one", 1, "two", 2, "three", 3, "four", 4, "five", 5) | "five"      | 5
        Parameters.of(new ParameterKey<Integer>("number", Integer), 7) | "number" | 7
    }

    def "supports immutable parameter updates and key queries"() {
        given:
        def parameters = Parameters.of("one", 1)

        expect:
        parameters.with("two", 2).get("two") == 2
        parameters.with(new ParameterKey<Integer>("two", Integer), 2).get("two") == 2
        !parameters.contains("two")
        parameters.containsAll(["one"] as Set)
        parameters.toString().startsWith("Parameters")
    }

    def "find returns a typed optional and convenience getters convert values"() {
        given:
        def parameters = Parameters.of(
                "amount", "pln 12.50",
                "decimal", 12,
                "date", "2025-01-02",
                "instant", "2025-01-02T03:04:05Z",
                "dateTime", "2025-01-02T03:04:05"
        )

        expect:
        parameters.find("amount", Money).get() == Money.of(new BigDecimal("12.50"), "PLN")
        parameters.find("missing", String).isEmpty()
        parameters.getMoney("amount") == Money.of(new BigDecimal("12.50"), "PLN")
        parameters.getBigDecimal("decimal") == new BigDecimal("12")
        parameters.getLocalDate("date") == LocalDate.of(2025, 1, 2)
        parameters.getInstant("instant") == Instant.parse("2025-01-02T03:04:05Z")
        parameters.getLocalDateTime("dateTime") == LocalDateTime.of(2025, 1, 2, 3, 4, 5)
    }

    def "typed values bypass conversion and absent values fail clearly"() {
        given:
        def parameters = Parameters.of("value", 12)

        expect:
        parameters.get("value", Integer) == 12
        parameters.find(new ParameterKey<Integer>("value", Integer)).get() == 12

        when:
        parameters.get("missing")

        then:
        def missing = thrown(IllegalArgumentException)
        missing.message.contains("missing")
    }

    def "conversion failures identify the parameter and expected type"() {
        given:
        def parameters = Parameters.of("amount", "not-money")

        when:
        parameters.get("amount", Money)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains("amount")
        failure.message.contains("Money")
        failure.cause instanceof IllegalArgumentException
    }

    def "unsupported and malformed values fail through the public API"() {
        when:
        Parameters.of("value", value).get("value", type)

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains("value")

        where:
        value        | type
        new Object() | String
        "invalid"    | BigDecimal
        "invalid"    | LocalDate
        "invalid"    | Instant
        "invalid"    | LocalDateTime
    }
}

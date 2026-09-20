package com.github.monaboiste.fairshare.pricing.component

import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import spock.lang.Specification

class ApplicabilityConstraintSpec extends Specification {

    def "always-true constraint is satisfied by any context"() {
        given:
        def constraint = ApplicabilityConstraint.alwaysTrue()

        expect:
        constraint.isSatisfiedBy(Parameters.empty())
        constraint.isSatisfiedBy(Parameters.of("anything", "value"))
    }

    def "equals-to constraint matches an exact string value"() {
        given:
        def constraint = ApplicabilityConstraint.equalsTo("cargo-type", "hazmat")

        expect:
        constraint.isSatisfiedBy(Parameters.of("cargo-type", "hazmat"))
        !constraint.isSatisfiedBy(Parameters.of("cargo-type", "standard"))
    }

    def "equals-to constraint rejects a context when the parameter is absent"() {
        given:
        def constraint = ApplicabilityConstraint.equalsTo("cargo-type", "hazmat")

        when:
        constraint.isSatisfiedBy(Parameters.empty())

        then:
        thrown(IllegalArgumentException)
    }

    def "in constraint matches any value from the allowed set"() {
        given:
        def constraint = ApplicabilityConstraint.in("zone", "A", "B", "C")

        expect:
        constraint.isSatisfiedBy(Parameters.of("zone", "A"))
        constraint.isSatisfiedBy(Parameters.of("zone", "C"))
        !constraint.isSatisfiedBy(Parameters.of("zone", "D"))
    }

    def "in constraint rejects a context when the parameter is absent"() {
        given:
        def constraint = ApplicabilityConstraint.in("zone", "A", "B")

        when:
        constraint.isSatisfiedBy(Parameters.empty())

        then:
        thrown(IllegalArgumentException)
    }

    def "greater-than constraint is satisfied strictly above the threshold"() {
        given:
        def constraint = ApplicabilityConstraint.greaterThan("weight", 10)

        expect:
        constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(11)))
        !constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(10)))
        !constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(9)))
    }

    def "greater-than constraint rejects a context when the parameter is absent"() {
        given:
        def constraint = ApplicabilityConstraint.greaterThan("weight", 10)

        when:
        constraint.isSatisfiedBy(Parameters.empty())

        then:
        thrown(IllegalArgumentException)
    }

    def "greater-than constraint returns false for a non-numeric value"() {
        given:
        def constraint = ApplicabilityConstraint.greaterThan("weight", 10)

        expect:
        !constraint.isSatisfiedBy(Parameters.of("weight", "heavy"))
    }

    def "greater-than-or-equal-to constraint is satisfied at and above the threshold"() {
        given:
        def constraint = ApplicabilityConstraint.greaterThanOrEqualTo("quantity", 5)

        expect:
        constraint.isSatisfiedBy(Parameters.of("quantity", BigDecimal.valueOf(5)))
        constraint.isSatisfiedBy(Parameters.of("quantity", BigDecimal.valueOf(10)))
        !constraint.isSatisfiedBy(Parameters.of("quantity", BigDecimal.valueOf(4)))
    }

    def "less-than constraint is satisfied strictly below the threshold"() {
        given:
        def constraint = ApplicabilityConstraint.lessThan("sessions", 5)

        expect:
        constraint.isSatisfiedBy(Parameters.of("sessions", BigDecimal.valueOf(4)))
        !constraint.isSatisfiedBy(Parameters.of("sessions", BigDecimal.valueOf(5)))
        !constraint.isSatisfiedBy(Parameters.of("sessions", BigDecimal.valueOf(6)))
    }

    def "less-than-or-equal-to constraint is satisfied at and below the threshold"() {
        given:
        def constraint = ApplicabilityConstraint.lessThanOrEqualTo("quantity", 100)

        expect:
        constraint.isSatisfiedBy(Parameters.of("quantity", BigDecimal.valueOf(100)))
        constraint.isSatisfiedBy(Parameters.of("quantity", BigDecimal.valueOf(50)))
        !constraint.isSatisfiedBy(Parameters.of("quantity", BigDecimal.valueOf(101)))
    }

    def "between constraint is satisfied within inclusive bounds"() {
        given:
        def constraint = ApplicabilityConstraint.between("weight", 5, 30)

        expect:
        constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(5)))
        constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(17)))
        constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(30)))
        !constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(4)))
        !constraint.isSatisfiedBy(Parameters.of("weight", BigDecimal.valueOf(31)))
    }

    def "between constraint rejects a context when the parameter is absent"() {
        given:
        def constraint = ApplicabilityConstraint.between("weight", 5, 30)

        when:
        constraint.isSatisfiedBy(Parameters.empty())

        then:
        thrown(IllegalArgumentException)
    }

    def "and constraint requires all nested constraints to be satisfied"() {
        given:
        def constraint = ApplicabilityConstraint.and(
                ApplicabilityConstraint.equalsTo("type", "B2C"),
                ApplicabilityConstraint.greaterThan("sessions", 10)
        )

        expect:
        constraint.isSatisfiedBy(Parameters.of("type", "B2C", "sessions", BigDecimal.valueOf(15)))
        !constraint.isSatisfiedBy(Parameters.of("type", "B2C", "sessions", BigDecimal.valueOf(5)))
        !constraint.isSatisfiedBy(Parameters.of("type", "B2B", "sessions", BigDecimal.valueOf(15)))
    }

    def "or constraint is satisfied when at least one nested constraint is satisfied"() {
        given:
        def constraint = ApplicabilityConstraint.or(
                ApplicabilityConstraint.equalsTo("status", "gold"),
                ApplicabilityConstraint.equalsTo("status", "platinum")
        )

        expect:
        constraint.isSatisfiedBy(Parameters.of("status", "gold"))
        constraint.isSatisfiedBy(Parameters.of("status", "platinum"))
        !constraint.isSatisfiedBy(Parameters.of("status", "silver"))
    }

    def "not constraint negates the wrapped constraint"() {
        given:
        def constraint = ApplicabilityConstraint.not(ApplicabilityConstraint.equalsTo("excluded", "true"))

        expect:
        constraint.isSatisfiedBy(Parameters.of("excluded", "false"))
        !constraint.isSatisfiedBy(Parameters.of("excluded", "true"))

        when:
        constraint.isSatisfiedBy(Parameters.empty())

        then:
        thrown(IllegalArgumentException)
    }

    def "or-of-ands deep composition evaluates correctly"() {
        given:
        def constraint = ApplicabilityConstraint.or(
                ApplicabilityConstraint.and(
                        ApplicabilityConstraint.equalsTo("type", "B2C"),
                        ApplicabilityConstraint.greaterThanOrEqualTo("weight", 5)),
                ApplicabilityConstraint.and(
                        ApplicabilityConstraint.equalsTo("type", "B2B"),
                        ApplicabilityConstraint.greaterThanOrEqualTo("weight", 3))
        )

        expect:
        constraint.isSatisfiedBy(Parameters.of("type", "B2C", "weight", BigDecimal.valueOf(7)))
        constraint.isSatisfiedBy(Parameters.of("type", "B2B", "weight", BigDecimal.valueOf(4)))
        !constraint.isSatisfiedBy(Parameters.of("type", "B2C", "weight", BigDecimal.valueOf(2)))
        !constraint.isSatisfiedBy(Parameters.of("type", "B2B", "weight", BigDecimal.valueOf(1)))
    }

    def "not-of-and combination evaluates correctly"() {
        given:
        def constraint = ApplicabilityConstraint.not(ApplicabilityConstraint.and(
                ApplicabilityConstraint.equalsTo("status", "gold"),
                ApplicabilityConstraint.greaterThanOrEqualTo("quantity", BigDecimal.valueOf(100))
        ))

        expect:
        constraint.isSatisfiedBy(Parameters.of("status", "silver", "quantity", BigDecimal.valueOf(50)))
        constraint.isSatisfiedBy(Parameters.of("status", "gold", "quantity", BigDecimal.valueOf(50)))
        !constraint.isSatisfiedBy(Parameters.of("status", "gold", "quantity", BigDecimal.valueOf(200)))
    }
}

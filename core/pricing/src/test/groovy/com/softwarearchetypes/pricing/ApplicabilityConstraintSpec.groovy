package com.softwarearchetypes.pricing

import spock.lang.Specification

class ApplicabilityConstraintSpec extends Specification {

    def "always-true constraint is satisfied by any context"() {

        expect:
        ApplicabilityConstraint.alwaysTrue().isSatisfiedBy(emptyCtx())
        ApplicabilityConstraint.alwaysTrue().isSatisfiedBy(ctx("anything", "value"))
    }

    def "equals-to constraint matches an exact string value"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.equalsTo("cargo-type", "hazmat")

        expect:
        constraint.isSatisfiedBy(ctx("cargo-type", "hazmat"))
        !(constraint.isSatisfiedBy(ctx("cargo-type", "standard")))
    }

    def "equals-to constraint returns false when the parameter is absent"() {

        expect:
        !(ApplicabilityConstraint.equalsTo("cargo-type", "hazmat").isSatisfiedBy(emptyCtx()))
    }

    def "in constraint matches any value from the allowed set"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.in("zone", "A", "B", "C")

        expect:
        constraint.isSatisfiedBy(ctx("zone", "A"))
        constraint.isSatisfiedBy(ctx("zone", "C"))
        !(constraint.isSatisfiedBy(ctx("zone", "D")))
    }

    def "in constraint returns false when the parameter is absent"() {
        expect:
        !(ApplicabilityConstraint.in("zone", "A", "B").isSatisfiedBy(emptyCtx()))
    }

    def "greater-than constraint is satisfied strictly above the threshold"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.greaterThan("weight", 10)

        expect:
        constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(11)))
        !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(10))))
        !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(9))))
    }

    def "greater-than constraint returns false when the parameter is absent"() {

        expect:
        !(ApplicabilityConstraint.greaterThan("weight", 10).isSatisfiedBy(emptyCtx()))
    }

    def "greater-than constraint returns false for a non-numeric value"() {
        expect:
        !(ApplicabilityConstraint.greaterThan("weight", 10).isSatisfiedBy(ctx("weight", "heavy")))
    }

    def "greater-than-or-equal-to constraint is satisfied at and above the threshold"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.greaterThanOrEqualTo("quantity", 5)

        expect:
        constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(5)))
        constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(10)))
        !(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(4))))
    }

    def "less-than constraint is satisfied strictly below the threshold"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.lessThan("sessions", 5)

        expect:
        constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(4)))
        !(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(5))))
        !(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(6))))
    }

    def "less-than-or-equal-to constraint is satisfied at and below the threshold"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.lessThanOrEqualTo("quantity", 100)

        expect:
        constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(100)))
        constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(50)))
        !(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(101))))
    }

    def "between constraint is satisfied within inclusive bounds"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.between("weight", 5, 30)

        expect:
        constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(5)))
        constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(17)))
        constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(30)))
        !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(4))))
        !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(31))))
    }

    def "between constraint returns false when the parameter is absent"() {

        expect:
        !(ApplicabilityConstraint.between("weight", 5, 30).isSatisfiedBy(emptyCtx()))
    }

    def "and constraint requires all nested constraints to be satisfied"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.and(
                ApplicabilityConstraint.equalsTo("type", "B2C"),
                ApplicabilityConstraint.greaterThan("sessions", 10)
        )

        expect:
        constraint.isSatisfiedBy(ctx("type", "B2C", "sessions", BigDecimal.valueOf(15)))
        !(constraint.isSatisfiedBy(ctx("type", "B2C", "sessions", BigDecimal.valueOf(5))))
        !(constraint.isSatisfiedBy(ctx("type", "B2B", "sessions", BigDecimal.valueOf(15))))
    }

    def "or constraint is satisfied when at least one nested constraint is satisfied"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.or(
                ApplicabilityConstraint.equalsTo("status", "gold"),
                ApplicabilityConstraint.equalsTo("status", "platinum")
        )

        expect:
        constraint.isSatisfiedBy(ctx("status", "gold"))
        constraint.isSatisfiedBy(ctx("status", "platinum"))
        !(constraint.isSatisfiedBy(ctx("status", "silver")))
    }

    def "not constraint negates the wrapped constraint"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.not(ApplicabilityConstraint.equalsTo("excluded", "true"))

        expect:
        constraint.isSatisfiedBy(ctx("excluded", "false"))
        constraint.isSatisfiedBy(emptyCtx())
        !(constraint.isSatisfiedBy(ctx("excluded", "true")))
    }

    def "or-of-ands deep composition evaluates correctly"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.or(
                ApplicabilityConstraint.and(ApplicabilityConstraint.equalsTo("type", "B2C"), ApplicabilityConstraint.greaterThanOrEqualTo("weight", 5)),
                ApplicabilityConstraint.and(ApplicabilityConstraint.equalsTo("type", "B2B"), ApplicabilityConstraint.greaterThanOrEqualTo("weight", 3))
        )

        expect:
        constraint.isSatisfiedBy(ctx("type", "B2C", "weight", BigDecimal.valueOf(7)))
        constraint.isSatisfiedBy(ctx("type", "B2B", "weight", BigDecimal.valueOf(4)))
        !(constraint.isSatisfiedBy(ctx("type", "B2C", "weight", BigDecimal.valueOf(2))))
        !(constraint.isSatisfiedBy(ctx("type", "B2B", "weight", BigDecimal.valueOf(1))))
    }

    def "not-of-and combination evaluates correctly"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.not(ApplicabilityConstraint.and(
                ApplicabilityConstraint.equalsTo("status", "gold"),
                ApplicabilityConstraint.greaterThanOrEqualTo("quantity", BigDecimal.valueOf(100))
        ))

        expect:
        constraint.isSatisfiedBy(ctx("status", "silver", "quantity", BigDecimal.valueOf(50)))
        constraint.isSatisfiedBy(ctx("status", "gold", "quantity", BigDecimal.valueOf(50)))
        !(constraint.isSatisfiedBy(ctx("status", "gold", "quantity", BigDecimal.valueOf(200))))
    }

    private static PricingContext emptyCtx() {
        return PricingContext.from(Parameters.empty())
    }

    private static PricingContext ctx(String k1, Object v1) {
        return PricingContext.from(Parameters.of(k1, v1))
    }

    private static PricingContext ctx(String k1, Object v1, String k2, Object v2) {
        return PricingContext.from(Parameters.of(k1, v1, k2, v2))
    }
}

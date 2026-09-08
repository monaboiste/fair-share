package com.softwarearchetypes.pricing

import static com.softwarearchetypes.pricing.ApplicabilityConstraint.alwaysTrue
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.and
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.between
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.equalsTo
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.greaterThan
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.greaterThanOrEqualTo
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.in
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.lessThan
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.lessThanOrEqualTo
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.not
import static com.softwarearchetypes.pricing.ApplicabilityConstraint.or

import java.math.BigDecimal
import spock.lang.Specification


class ApplicabilityConstraintSpec extends Specification {
    def "alwaysTrue isSatisfiedByAnyContext"() {
        given:
        assert alwaysTrue().isSatisfiedBy(emptyCtx())
        assert alwaysTrue().isSatisfiedBy(ctx("anything", "value"))
    }
    def "equalsTo matchesExactStringValue"() {
        given:
        ApplicabilityConstraint constraint = equalsTo("cargo-type", "hazmat")

        assert constraint.isSatisfiedBy(ctx("cargo-type", "hazmat"))
        assert !(constraint.isSatisfiedBy(ctx("cargo-type", "standard")))
    }
    def "equalsTo returnsFalseWhenParameterAbsent"() {
        given:
        assert !(equalsTo("cargo-type", "hazmat").isSatisfiedBy(emptyCtx()))
    }
    def "in matchesAnyValueFromAllowedSet"() {
        given:
        ApplicabilityConstraint constraint = in("zone", "A", "B", "C")

        assert constraint.isSatisfiedBy(ctx("zone", "A"))
        assert constraint.isSatisfiedBy(ctx("zone", "C"))
        assert !(constraint.isSatisfiedBy(ctx("zone", "D")))
    }
    def "in returnsFalseWhenParameterAbsent"() {
        given:
        assert !(in("zone", "A", "B").isSatisfiedBy(emptyCtx()))
    }
    def "greaterThan isSatisfiedStrictlyAboveThreshold"() {
        given:
        ApplicabilityConstraint constraint = greaterThan("weight", 10)

        assert constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(11)))
        assert !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(10))))
        assert !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(9))))
    }
    def "greaterThan returnsFalseWhenParameterAbsent"() {
        given:
        assert !(greaterThan("weight", 10).isSatisfiedBy(emptyCtx()))
    }
    def "greaterThan returnsFalseForNonNumericValue"() {
        given:
        assert !(greaterThan("weight", 10).isSatisfiedBy(ctx("weight", "heavy")))
    }
    def "greaterThanOrEqualTo isSatisfiedAtAndAboveThreshold"() {
        given:
        ApplicabilityConstraint constraint = greaterThanOrEqualTo("quantity", 5)

        assert constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(5)))
        assert constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(10)))
        assert !(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(4))))
    }
    def "lessThan isSatisfiedStrictlyBelowThreshold"() {
        given:
        ApplicabilityConstraint constraint = lessThan("sessions", 5)

        assert constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(4)))
        assert !(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(5))))
        assert !(constraint.isSatisfiedBy(ctx("sessions", BigDecimal.valueOf(6))))
    }
    def "lessThanOrEqualTo isSatisfiedAtAndBelowThreshold"() {
        given:
        ApplicabilityConstraint constraint = lessThanOrEqualTo("quantity", 100)

        assert constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(100)))
        assert constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(50)))
        assert !(constraint.isSatisfiedBy(ctx("quantity", BigDecimal.valueOf(101))))
    }
    def "between isSatisfiedWithinInclusiveBounds"() {
        given:
        ApplicabilityConstraint constraint = between("weight", 5, 30)

        assert constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(5)))
        assert constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(17)))
        assert constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(30)))
        assert !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(4))))
        assert !(constraint.isSatisfiedBy(ctx("weight", BigDecimal.valueOf(31))))
    }
    def "between returnsFalseWhenParameterAbsent"() {
        given:
        assert !(between("weight", 5, 30).isSatisfiedBy(emptyCtx()))
    }
    def "and requiresAllConstraintsSatisfied"() {
        given:
        ApplicabilityConstraint constraint = and(
                equalsTo("type", "B2C"),
                greaterThan("sessions", 10)
        )

        assert constraint.isSatisfiedBy(ctx("type", "B2C", "sessions", BigDecimal.valueOf(15)))
        assert !(constraint.isSatisfiedBy(ctx("type", "B2C", "sessions", BigDecimal.valueOf(5))))
        assert !(constraint.isSatisfiedBy(ctx("type", "B2B", "sessions", BigDecimal.valueOf(15))))
    }
    def "or isSatisfiedByAtLeastOneConstraint"() {
        given:
        ApplicabilityConstraint constraint = or(
                equalsTo("status", "gold"),
                equalsTo("status", "platinum")
        )

        assert constraint.isSatisfiedBy(ctx("status", "gold"))
        assert constraint.isSatisfiedBy(ctx("status", "platinum"))
        assert !(constraint.isSatisfiedBy(ctx("status", "silver")))
    }
    def "not negatesConstraint"() {
        given:
        ApplicabilityConstraint constraint = not(equalsTo("excluded", "true"))

        assert constraint.isSatisfiedBy(ctx("excluded", "false"))
        assert constraint.isSatisfiedBy(emptyCtx())
        assert !(constraint.isSatisfiedBy(ctx("excluded", "true")))
    }
    def "shouldSupportDeepComposition orOfAnds"() {
        given:
        ApplicabilityConstraint constraint = or(
                and(equalsTo("type", "B2C"), greaterThanOrEqualTo("weight", 5)),
                and(equalsTo("type", "B2B"), greaterThanOrEqualTo("weight", 3))
        )

        assert constraint.isSatisfiedBy(ctx("type", "B2C", "weight", BigDecimal.valueOf(7)))
        assert constraint.isSatisfiedBy(ctx("type", "B2B", "weight", BigDecimal.valueOf(4)))
        assert !(constraint.isSatisfiedBy(ctx("type", "B2C", "weight", BigDecimal.valueOf(2))))
        assert !(constraint.isSatisfiedBy(ctx("type", "B2B", "weight", BigDecimal.valueOf(1))))
    }
    def "shouldSupportNotOfAnd"() {
        given:
        ApplicabilityConstraint constraint = not(and(
                equalsTo("status", "gold"),
                greaterThanOrEqualTo("quantity", BigDecimal.valueOf(100))
        ))

        assert constraint.isSatisfiedBy(ctx("status", "silver", "quantity", BigDecimal.valueOf(50)))
        assert constraint.isSatisfiedBy(ctx("status", "gold",   "quantity", BigDecimal.valueOf(50)))
        assert !(constraint.isSatisfiedBy(ctx("status", "gold",   "quantity", BigDecimal.valueOf(200))))
    }

    private PricingContext emptyCtx() {
        return PricingContext.from(Parameters.empty())
    }

    private PricingContext ctx(String k1, Object v1) {
        return PricingContext.from(Parameters.of(k1, v1))
    }

    private PricingContext ctx(String k1, Object v1, String k2, Object v2) {
        return PricingContext.from(Parameters.of(k1, v1, k2, v2))
    }
}

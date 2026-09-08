package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class ApplicabilityConstraintSpec extends Specification {

    def "equals to constraint is #satisfied when #scenario"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.equalsTo("country", "PL")
        ApplicabilityContext context = applicabilityContext

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        scenario            | applicabilityContext                             | expected | satisfied
        "value matches"     | ApplicabilityContext.of(Map.of("country", "PL")) | true     | "satisfied"
        "value differs"     | ApplicabilityContext.of(Map.of("country", "UK")) | false    | "not satisfied"
        "parameter missing" | ApplicabilityContext.empty()                     | false    | "not satisfied"
    }

    def "in constraint is #satisfied when value is #value"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.in("channel", "mobile", "web", "tablet")
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", value))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        value     | expected | satisfied
        "mobile"  | true     | "satisfied"
        "desktop" | false    | "not satisfied"
    }

    def "greater than constraint is #satisfied when value is #value"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.greaterThan("age", 18)
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", value))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        value   | expected | satisfied
        "25"    | true     | "satisfied"
        "18"    | false    | "not satisfied"
        "15"    | false    | "not satisfied"
        "adult" | false    | "not satisfied"
    }

    def "less than constraint is #satisfied when value is #value"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.lessThan("age", 16)
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", value))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        value | expected | satisfied
        "12"  | true     | "satisfied"
        "20"  | false    | "not satisfied"
    }

    def "between constraint is #satisfied when value is #value"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.between("age", 18, 65)
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", value))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        value | expected | satisfied
        "30"  | true     | "satisfied"
        "18"  | true     | "satisfied"
        "65"  | true     | "satisfied"
        "70"  | false    | "not satisfied"
    }

    def "and constraint is #satisfied when channel is #channel"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.and(
                ApplicabilityConstraint.equalsTo("country", "PL"),
                ApplicabilityConstraint.equalsTo("channel", "mobile")
        )
        ApplicabilityContext context = ApplicabilityContext.of(Map.of(
                "country", "PL",
                "channel", channel
        ))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        channel  | expected | satisfied
        "mobile" | true     | "satisfied"
        "web"    | false    | "not satisfied"
    }

    def "or constraint is #satisfied when country is #country"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.or(
                ApplicabilityConstraint.equalsTo("country", "PL"),
                ApplicabilityConstraint.equalsTo("country", "UK")
        )
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", country))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        country | expected | satisfied
        "UK"    | true     | "satisfied"
        "DE"    | false    | "not satisfied"
    }

    def "not constraint is #satisfied when country is #country"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.not(ApplicabilityConstraint.equalsTo("country", "PL"))
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("country", country))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        country | expected | satisfied
        "UK"    | true     | "satisfied"
        "PL"    | false    | "not satisfied"
    }

    def "complex nested constraint is #satisfied when age is #age"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.and(
                ApplicabilityConstraint.or(ApplicabilityConstraint.equalsTo("country", "PL"), ApplicabilityConstraint.equalsTo("country", "UK")),
                ApplicabilityConstraint.or(ApplicabilityConstraint.equalsTo("channel", "mobile"), ApplicabilityConstraint.equalsTo("channel", "web")),
                ApplicabilityConstraint.lessThan("age", 16)
        )
        ApplicabilityContext context = ApplicabilityContext.of(Map.of(
                "country", "UK",
                "channel", "mobile",
                "age", age
        ))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        age  | expected | satisfied
        "12" | true     | "satisfied"
        "18" | false    | "not satisfied"
    }

    def "always true constraint is satisfied with #scenario context"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.alwaysTrue()
        ApplicabilityContext context = applicabilityContext

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result

        where:
        scenario    | applicabilityContext
        "empty"     | ApplicabilityContext.empty()
        "populated" | ApplicabilityContext.of(Map.of("country", "PL", "channel", "mobile"))
    }

    def "product applicability constraint is #satisfied for #channel channel"() {
        given:
        ProductType mobileOnlyProduct = ProductType.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Mobile App Premium"),
                ProductDescription.of("Premium feature available only on mobile"),
                Unit.pieces(),
                ProductTrackingStrategy.IDENTICAL
        )
                .withApplicabilityConstraint(ApplicabilityConstraint.equalsTo("channel", "mobile"))
                .build()
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("channel", channel))

        when:
        boolean result = mobileOnlyProduct.isApplicableFor(context)

        then:
        result == expected

        where:
        channel  | expected | satisfied
        "mobile" | true     | "satisfied"
        "web"    | false    | "not satisfied"
    }

    def "complex product applicability constraint is #satisfied for #scenario"() {
        given:
        ProductType pediatricProduct = ProductType.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Pediatric Service"),
                ProductDescription.of("Service for children"),
                Unit.pieces(),
                ProductTrackingStrategy.IDENTICAL
        )
                .withApplicabilityConstraint(
                        ApplicabilityConstraint.and(
                                ApplicabilityConstraint.or(ApplicabilityConstraint.equalsTo("country", "PL"), ApplicabilityConstraint.equalsTo("country", "UK")),
                                ApplicabilityConstraint.or(ApplicabilityConstraint.equalsTo("channel", "mobile"), ApplicabilityConstraint.equalsTo("channel", "web")),
                                ApplicabilityConstraint.lessThan("age", 16)
                        )
                )
                .build()
        ApplicabilityContext context = ApplicabilityContext.of(parameters)

        when:
        boolean result = pediatricProduct.isApplicableFor(context)

        then:
        result == expected

        where:
        scenario          | parameters                                                | expected | satisfied
        "eligible user"   | Map.of("country", "PL", "channel", "mobile", "age", "10") | true     | "satisfied"
        "adult user"      | Map.of("country", "PL", "channel", "mobile", "age", "18") | false    | "not satisfied"
        "invalid country" | Map.of("country", "DE", "channel", "mobile", "age", "10") | false    | "not satisfied"
    }

    def "default constraint is always satisfied for #scenario context"() {
        given:
        ProductType product = ProductType.identical(
                UuidProductIdentifier.random(),
                ProductName.of("Universal Product"),
                ProductDescription.of("No restrictions"),
                Unit.pieces()
        )
        ApplicabilityContext context = applicabilityContext

        when:
        boolean result = product.isApplicableFor(context)

        then:
        result

        where:
        scenario            | applicabilityContext
        "empty"             | ApplicabilityContext.empty()
        "with country"      | ApplicabilityContext.of(Map.of("country", "PL"))
        "with user details" | ApplicabilityContext.of(Map.of("channel", "mobile", "age", "99"))
    }

    def "combined in constraints are #satisfied for country #country"() {
        given:
        ApplicabilityConstraint constraint = ApplicabilityConstraint.and(
                ApplicabilityConstraint.in("country", "PL", "UK", "DE"),
                ApplicabilityConstraint.in("channel", "mobile", "web")
        )
        ApplicabilityContext context = ApplicabilityContext.of(Map.of(
                "country", country,
                "channel", "web"
        ))

        when:
        boolean result = constraint.isSatisfiedBy(context)

        then:
        result == expected

        where:
        country | expected | satisfied
        "DE"    | true     | "satisfied"
        "FR"    | false    | "not satisfied"
    }

    def "range constrained product is #satisfied for age #age"() {
        given:
        ProductType ageRestrictedProduct = ProductType.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Teen Product"),
                ProductDescription.of("For teenagers only"),
                Unit.pieces(),
                ProductTrackingStrategy.IDENTICAL
        )
                .withApplicabilityConstraint(ApplicabilityConstraint.between("age", 13, 19))
                .build()
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("age", age))

        when:
        boolean result = ageRestrictedProduct.isApplicableFor(context)

        then:
        result == expected

        where:
        age  | expected | satisfied
        "15" | true     | "satisfied"
        "10" | false    | "not satisfied"
        "25" | false    | "not satisfied"
    }
}

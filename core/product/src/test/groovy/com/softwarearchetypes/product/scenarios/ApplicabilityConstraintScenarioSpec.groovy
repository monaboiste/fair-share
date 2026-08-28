package com.softwarearchetypes.product.scenarios

import com.softwarearchetypes.product.ApplicabilityConstraint
import com.softwarearchetypes.product.ApplicabilityContext
import com.softwarearchetypes.product.Product
import com.softwarearchetypes.product.ProductDescription
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductName
import com.softwarearchetypes.product.ProductTrackingStrategy
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class ApplicabilityConstraintScenarioSpec extends Specification {

    def "mobile premium is available only through the mobile channel"() {
        given:
        ProductType product = product(
                "Mobile App Premium",
                ApplicabilityConstraint.equalsTo("channel", "mobile"))

        when:
        boolean applicable = product.isApplicableFor(ApplicabilityContext.of(Map.of("channel", channel)))

        then:
        applicable == expected

        where:
        channel   | expected
        "mobile"  | true
        "web"     | false
        "desktop" | false
    }

    def "pediatric service requires an eligible country channel and age"() {
        given:
        ProductType product = product(
                "Pediatric Service",
                ApplicabilityConstraint.and(
                        ApplicabilityConstraint.in("country", "PL", "UK"),
                        ApplicabilityConstraint.in("channel", "mobile", "web"),
                        ApplicabilityConstraint.lessThan("age", 16)))

        when:
        boolean applicable = product.isApplicableFor(ApplicabilityContext.of(Map.of(
                "country", country,
                "channel", channel,
                "age", age)))

        then:
        applicable == expected

        where:
        country | channel   | age  | expected
        "PL"    | "mobile"  | "10" | true
        "PL"    | "mobile"  | "18" | false
        "DE"    | "mobile"  | "10" | false
        "PL"    | "desktop" | "10" | false
        "UK"    | "web"     | "15" | true
    }

    def "teen product is available only within the inclusive age range"() {
        given:
        ProductType product = product("Teen Product", ApplicabilityConstraint.between("age", 13, 19))

        when:
        boolean applicable = product.isApplicableFor(ApplicabilityContext.of(Map.of("age", age)))

        then:
        applicable == expected

        where:
        age  | expected
        "13" | true
        "15" | true
        "19" | true
        "10" | false
        "25" | false
    }

    def "unrestricted product is available in every context"() {
        given:
        ProductType product = product("Universal Product", ApplicabilityConstraint.alwaysTrue())

        when:
        boolean applicable = product.isApplicableFor(context)

        then:
        applicable

        where:
        context << [
                ApplicabilityContext.empty(),
                ApplicabilityContext.of(Map.of("country", "PL")),
                ApplicabilityContext.of(Map.of("channel", "mobile", "age", "99", "country", "ZZ"))
        ]
    }

    def "regional product requires an eligible country and channel"() {
        given:
        ProductType product = product(
                "Regional Product",
                ApplicabilityConstraint.and(
                        ApplicabilityConstraint.in("country", "PL", "UK", "DE"),
                        ApplicabilityConstraint.in("channel", "mobile", "web")))

        when:
        boolean applicable = product.isApplicableFor(ApplicabilityContext.of(Map.of(
                "country", country,
                "channel", channel)))

        then:
        applicable == expected

        where:
        country | channel   | expected
        "DE"    | "web"     | true
        "FR"    | "web"     | false
        "DE"    | "desktop" | false
    }

    def "desktop product excludes the mobile channel"() {
        given:
        ProductType product = product(
                "Desktop Product",
                ApplicabilityConstraint.not(ApplicabilityConstraint.equalsTo("channel", "mobile")))

        when:
        boolean applicable = product.isApplicableFor(ApplicabilityContext.of(Map.of("channel", channel)))

        then:
        applicable == expected

        where:
        channel   | expected
        "web"     | true
        "desktop" | true
        "mobile"  | false
    }

    def "special offer accepts premium customers or promotional countries"() {
        given:
        ProductType product = product(
                "Special Offer",
                ApplicabilityConstraint.or(
                        ApplicabilityConstraint.equalsTo("customerTier", "premium"),
                        ApplicabilityConstraint.in("country", "PL", "CZ", "SK")))

        when:
        boolean applicable = product.isApplicableFor(ApplicabilityContext.of(Map.of(
                "customerTier", customerTier,
                "country", country)))

        then:
        applicable == expected

        where:
        customerTier | country | expected
        "premium"    | "US"    | true
        "regular"    | "PL"    | true
        "regular"    | "DE"    | false
    }

    private static ProductType product(String name, ApplicabilityConstraint constraint) {
        return Product.builder(
                        ProductIdentifier.of(UUID.randomUUID().toString()),
                        ProductName.of(name),
                        ProductDescription.of(name))
                .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                .withApplicabilityConstraint(constraint)
                .build()
    }
}

package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.quantity.money.Money
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import spock.lang.Specification

class CompositeComponentApplicabilitySpec extends Specification {

    private final Map<String, Component> components = [:]

    def setup() {
        components["fixed-100"] = SimpleComponent.of("fixed-100", Calculators.fixed("fixed-100", Money.of(100, "PLN")))
        components["fixed-50"] = SimpleComponent.of("fixed-50", Calculators.fixed("fixed-50", Money.of(50, "PLN")))
        components["fixed-30"] = SimpleComponent.of("fixed-30", Calculators.fixed("fixed-30", Money.of(30, "PLN")))
        components["fixed-20"] = SimpleComponent.of("fixed-20", Calculators.fixed("fixed-20", Money.of(20, "PLN")))
        components["pct-10"] = SimpleComponent.of("pct-10", Calculators.percentage("pct-10", 10G))
    }

    private void simple(String name, String calculator, ApplicabilityConstraint constraint = ApplicabilityConstraint.alwaysTrue()) {
        def source = components[calculator] as SimpleComponent
        components[name] = SimpleComponent.withInitialVersion(name, source.versions()[0].calculator(), Map.of(), constraint, Validity.always(), java.time.Clock.systemUTC())
    }

    private void composite(String name, Map deps, String... children) {
        composite(name, deps, ApplicabilityConstraint.alwaysTrue(), Validity.always(), children)
    }

    private void composite(String name, Map deps, ApplicabilityConstraint constraint, String... children) {
        composite(name, deps, constraint, Validity.always(), children)
    }

    private void composite(String name, Map deps = Map.of(), ApplicabilityConstraint constraint = ApplicabilityConstraint.alwaysTrue(), Validity validity = Validity.always(), String... children) {
        components[name] = CompositeComponent.withInitialVersion(name, children.collect { components[it] }, deps, constraint, validity, java.time.Clock.systemUTC())
    }

    private Money calculate(String name, Parameters params) { components[name].calculate(params).money() }
    private ComponentBreakdown breakdown(String name, Parameters params) { components[name].calculateBreakdown(params) }

    def "composite component returns zero when its applicability constraint is not satisfied"() {
        given:
        simple("base-fee", "fixed-100")
        simple("premium-feature", "fixed-50")
        simple("loyalty-bonus", "fixed-30")

        composite("premium-bundle",
                Map.of(),
                ApplicabilityConstraint.equalsTo("customer-type", "premium"),
                "premium-feature", "loyalty-bonus")

        composite("total",
                Map.of(),
                "base-fee", "premium-bundle")

        Parameters standard = Parameters.of("customer-type", "standard")
        Parameters premium = Parameters.of("customer-type", "premium")

        expect:
        calculate("total", standard) == Money.of(BigDecimal.valueOf(100), "PLN")
        calculate("total", premium) == Money.of(BigDecimal.valueOf(180), "PLN")
    }

    def "composite component appears in the breakdown with zero when not applicable"() {
        given:
        simple("base-fee", "fixed-100")
        simple("premium-feature", "fixed-50")
        simple("loyalty-bonus", "fixed-30")

        composite("premium-bundle",
                Map.of(),
                ApplicabilityConstraint.equalsTo("customer-type", "premium"),
                "premium-feature", "loyalty-bonus")

        composite("total",
                Map.of(),
                "base-fee", "premium-bundle")

        Parameters standard = Parameters.of("customer-type", "standard")
        ComponentBreakdown breakdown = breakdown("total", standard)

        expect:
        breakdown.total() == Money.of(BigDecimal.valueOf(100), "PLN")
    }

    def "numeric applicability constraint selects the correct composite"() {
        given:
        simple("light-fee", "fixed-20")
        simple("heavy-fee", "fixed-50")

        composite("light-delivery",
                Map.of(),
                ApplicabilityConstraint.lessThan("weight", 5),
                "light-fee")

        composite("heavy-delivery",
                Map.of(),
                ApplicabilityConstraint.greaterThanOrEqualTo("weight", 5),
                "heavy-fee")

        composite("delivery-cost",
                Map.of(),
                "light-delivery", "heavy-delivery")

        Parameters light = Parameters.of("weight", BigDecimal.valueOf(3))
        Parameters heavy = Parameters.of("weight", BigDecimal.valueOf(10))

        expect:
        calculate("delivery-cost", light) == Money.of(BigDecimal.valueOf(20), "PLN")
        calculate("delivery-cost", heavy) == Money.of(BigDecimal.valueOf(50), "PLN")
    }

    def "weight at the active boundary falls into the correct composite"() {
        given:
        simple("light-fee", "fixed-20")
        simple("heavy-fee", "fixed-50")

        composite("light-delivery",
                Map.of(), ApplicabilityConstraint.lessThan("weight", 5), "light-fee")
        composite("heavy-delivery",
                Map.of(), ApplicabilityConstraint.greaterThanOrEqualTo("weight", 5), "heavy-fee")
        composite("delivery-cost",
                Map.of(), "light-delivery", "heavy-delivery")

        Parameters boundary = Parameters.of("weight", BigDecimal.valueOf(5))

        expect:
        calculate("delivery-cost", boundary) == Money.of(BigDecimal.valueOf(50), "PLN")
    }

    def "outer composite constraint gates the entire subtree"() {
        given:
        simple("handling-fee", "fixed-100")
        simple("inspection-fee", "fixed-50",
                ApplicabilityConstraint.equalsTo("zone", "restricted"))

        composite("hazmat-package",
                Map.of(),
                ApplicabilityConstraint.equalsTo("cargo", "hazmat"),
                "handling-fee", "inspection-fee")

        composite("contract",
                Map.of(),
                "hazmat-package")

        Parameters hazmatRestricted = Parameters.of("cargo", "hazmat", "zone", "restricted")
        Parameters hazmatStandard = Parameters.of("cargo", "hazmat", "zone", "standard")
        Parameters normalCargo = Parameters.of("cargo", "standard", "zone", "restricted")

        expect:
        calculate("contract", hazmatRestricted) == Money.of(BigDecimal.valueOf(150), "PLN")
        calculate("contract", hazmatStandard) == Money.of(BigDecimal.valueOf(100), "PLN")
        calculate("contract", normalCargo) == Money.of(BigDecimal.ZERO, "PLN")
    }

    def "composite requires both a valid period and a satisfied constraint to contribute"() {
        given:
        simple("promo-fee", "fixed-50")
        simple("bonus-fee", "fixed-30")

        composite("promo-bundle",
                Map.of(),
                ApplicabilityConstraint.equalsTo("member", "gold"),
                Validity.between(
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 7, 1, 0, 0)),
                "promo-fee", "bonus-fee")

        composite("total",
                Map.of(),
                "promo-bundle")
        Parameters withinGold = Parameters.of("member", "gold")
                .with("timestamp", LocalDateTime.of(2025, 6, 15, 10, 0))

        expect:
        calculate("total", withinGold) == Money.of(BigDecimal.valueOf(80), "PLN")
        Parameters withinSilver = Parameters.of("member", "silver")
                .with("timestamp", LocalDateTime.of(2025, 6, 15, 10, 0))
        calculate("total", withinSilver) == Money.of(BigDecimal.ZERO, "PLN")
    }

    def "composite does not compute children when its constraint is not satisfied"() {
        given:
        simple("base-service", "fixed-100")
        simple("surcharge", "pct-10")

        composite("surcharge-bundle",
                Map.of(),
                ApplicabilityConstraint.equalsTo("tier", "enterprise"),
                "surcharge")

        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterValue>> of(
                "surcharge-bundle", Map.of("baseAmount", ParameterExpression.valueOf("base-service")))
        composite("service-cost", dependencies,
                "base-service", "surcharge-bundle")

        Parameters enterprise = Parameters.of("tier", "enterprise")
        Parameters standard = Parameters.of("tier", "standard")

        expect:
        calculate("service-cost", enterprise) == Money.of(BigDecimal.valueOf(110), "PLN")
        calculate("service-cost", standard) == Money.of(BigDecimal.valueOf(100), "PLN")
    }
}

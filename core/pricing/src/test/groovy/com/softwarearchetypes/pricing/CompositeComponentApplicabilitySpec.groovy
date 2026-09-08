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
import static com.softwarearchetypes.pricing.ComponentBreakdownAssert.assertThat
import static java.time.Clock.fixed

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Map
import spock.lang.Specification


class CompositeComponentApplicabilitySpec extends Specification {

    static final Instant NOW = LocalDateTime.of(2025, 6, 1, 12, 0).atZone(ZoneId.systemDefault()).toInstant()
    static final Clock clock = fixed(NOW, ZoneId.systemDefault())

    private PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()
    def setup() {
        facade.addCalculator("fixed-100", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(100), "PLN")))
        facade.addCalculator("fixed-50", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(50), "PLN")))
        facade.addCalculator("fixed-30", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(30), "PLN")))
        facade.addCalculator("fixed-20", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(20), "PLN")))
        facade.addCalculator("pct-10", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(10)))
    }
    def "shouldReturnZeroForCompositeWhenConstraintNotSatisfied"() {
        given:
        facade.createSimpleComponent("base-fee", "fixed-100")
        facade.createSimpleComponent("premium-feature", "fixed-50")
        facade.createSimpleComponent("loyalty-bonus", "fixed-30")

        facade.createCompositeComponent("premium-bundle",
                Map.of(),
                equalsTo("customer-type", "premium"),
                "premium-feature", "loyalty-bonus")

        facade.createCompositeComponent("total",
                Map.of(),
                "base-fee", "premium-bundle")

        Parameters standard = Parameters.of("customer-type", "standard")
        Parameters premium  = Parameters.of("customer-type", "premium")

        assert facade.calculateComponent("total", standard) == Money.of(BigDecimal.valueOf(100), "PLN")
        assert facade.calculateComponent("total", premium) == Money.of(BigDecimal.valueOf(180), "PLN")
    }
    def "shouldIncludeCompositeInBreakdownWithZeroWhenNotApplicable"() {
        given:
        facade.createSimpleComponent("base-fee", "fixed-100")
        facade.createSimpleComponent("premium-feature", "fixed-50")
        facade.createSimpleComponent("loyalty-bonus", "fixed-30")

        facade.createCompositeComponent("premium-bundle",
                Map.of(),
                equalsTo("customer-type", "premium"),
                "premium-feature", "loyalty-bonus")

        facade.createCompositeComponent("total",
                Map.of(),
                "base-fee", "premium-bundle")

        Parameters standard = Parameters.of("customer-type", "standard")
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total", standard)

        assertThat(breakdown).hasTotal(Money.of(BigDecimal.valueOf(100), "PLN"))
        assertThat(breakdown).child("base-fee").hasTotal(Money.of(BigDecimal.valueOf(100), "PLN"))
        assertThat(breakdown).child("premium-bundle").hasTotal(Money.of(BigDecimal.ZERO, "PLN"))
    }
    def "shouldSelectCorrectCompositeBasedOnNumericConstraint"() {
        given:
        facade.createSimpleComponent("light-fee", "fixed-20")
        facade.createSimpleComponent("heavy-fee", "fixed-50")

        facade.createCompositeComponent("light-delivery",
                Map.of(),
                lessThan("weight", 5),
                "light-fee")

        facade.createCompositeComponent("heavy-delivery",
                Map.of(),
                greaterThanOrEqualTo("weight", 5),
                "heavy-fee")

        facade.createCompositeComponent("delivery-cost",
                Map.of(),
                "light-delivery", "heavy-delivery")

        Parameters light = Parameters.of("weight", BigDecimal.valueOf(3))
        Parameters heavy = Parameters.of("weight", BigDecimal.valueOf(10))

        assert facade.calculateComponent("delivery-cost", light) == Money.of(BigDecimal.valueOf(20), "PLN")
        assert facade.calculateComponent("delivery-cost", heavy) == Money.of(BigDecimal.valueOf(50), "PLN")
    }
    def "shouldReturnZeroForBothCompositesWhenWeightAtNoBoundary"() {
        given:
        facade.createSimpleComponent("light-fee", "fixed-20")
        facade.createSimpleComponent("heavy-fee", "fixed-50")

        facade.createCompositeComponent("light-delivery",
                Map.of(), lessThan("weight", 5), "light-fee")
        facade.createCompositeComponent("heavy-delivery",
                Map.of(), greaterThanOrEqualTo("weight", 5), "heavy-fee")
        facade.createCompositeComponent("delivery-cost",
                Map.of(), "light-delivery", "heavy-delivery")

        Parameters boundary = Parameters.of("weight", BigDecimal.valueOf(5))
        assert facade.calculateComponent("delivery-cost", boundary) == Money.of(BigDecimal.valueOf(50), "PLN")
    }
    def "shouldGateEntireSubtreeWithOuterCompositeConstraint"() {
        given:
        facade.createSimpleComponent("handling-fee", "fixed-100")
        facade.createSimpleComponent("inspection-fee", "fixed-50",
                equalsTo("zone", "restricted"))

        facade.createCompositeComponent("hazmat-package",
                Map.of(),
                equalsTo("cargo", "hazmat"),
                "handling-fee", "inspection-fee")

        facade.createCompositeComponent("contract",
                Map.of(),
                "hazmat-package")

        Parameters hazmatRestricted = Parameters.of("cargo", "hazmat", "zone", "restricted")
        Parameters hazmatStandard   = Parameters.of("cargo", "hazmat",   "zone", "standard")
        Parameters normalCargo      = Parameters.of("cargo", "standard", "zone", "restricted")

        assert facade.calculateComponent("contract", hazmatRestricted) == Money.of(BigDecimal.valueOf(150), "PLN")
        assert facade.calculateComponent("contract", hazmatStandard) == Money.of(BigDecimal.valueOf(100), "PLN")
        assert facade.calculateComponent("contract", normalCargo) == Money.of(BigDecimal.ZERO, "PLN")
    }
    def "shouldRequireBothValidityAndConstraintForComposite"() {
        given:
        facade.createSimpleComponent("promo-fee", "fixed-50")
        facade.createSimpleComponent("bonus-fee", "fixed-30")

        facade.createCompositeComponent("promo-bundle",
                Map.of(),
                equalsTo("member", "gold"),
                Validity.between(
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2025, 7, 1, 0, 0)),
                "promo-fee", "bonus-fee")

        facade.createCompositeComponent("total",
                Map.of(),
                "promo-bundle")
        Parameters withinGold = Parameters.of("member", "gold")
                .with("timestamp", LocalDateTime.of(2025, 6, 15, 10, 0))
        assert facade.calculateComponent("total", withinGold) == Money.of(BigDecimal.valueOf(80), "PLN")
        Parameters withinSilver = Parameters.of("member", "silver")
                .with("timestamp", LocalDateTime.of(2025, 6, 15, 10, 0))
        assert facade.calculateComponent("total", withinSilver) == Money.of(BigDecimal.ZERO, "PLN")
    }
    def "shouldNotComputeChildrenWhenCompositeConstraintNotSatisfied"() {
        given:
        facade.createSimpleComponent("base-service", "fixed-100")
        facade.createSimpleComponent("surcharge", "pct-10")

        facade.createCompositeComponent("surcharge-bundle",
                Map.of(),
                equalsTo("tier", "enterprise"),
                "surcharge")

        facade.createCompositeComponent("service-cost",
                Map.of("surcharge-bundle", Map.of("baseAmount", new ValueOf("base-service"))),
                "base-service", "surcharge-bundle")

        Parameters enterprise = Parameters.of("tier", "enterprise")
        Parameters standard   = Parameters.of("tier", "standard")
        assert facade.calculateComponent("service-cost", enterprise) == Money.of(BigDecimal.valueOf(110), "PLN")
        assert facade.calculateComponent("service-cost", standard) == Money.of(BigDecimal.valueOf(100), "PLN")
    }
}

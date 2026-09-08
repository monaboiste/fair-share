package com.softwarearchetypes.pricing

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


class TelcoComponentScenarioSpec extends Specification {

    static final Instant NOW = LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant()
    static final Clock clock = fixed(NOW, ZoneId.systemDefault())
    private PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()
    def setup() {
        facade.addCalculator("network-maintenance", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(25), "PLN")))

        facade.addCalculator("commission", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(20), "PLN")))

        facade.addCalculator("data-overage", CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(BigDecimal.ZERO, "PLN"),
                        "stepSize", BigDecimal.ONE,
                        "stepIncrement", BigDecimal.valueOf(2)
                ))

        facade.addCalculator("roaming-overage", CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(BigDecimal.ZERO, "PLN"),
                        "stepSize", BigDecimal.ONE,
                        "stepIncrement", BigDecimal.valueOf(1.5)
                ))

        facade.addCalculator("percentage-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(23)))
    }
    def "shouldCalculateMonthlyBillWithBaseFeesOnly"() {
        given:
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance")
        facade.createSimpleComponent("commission-component", "commission")

        facade.createCompositeComponent(
                "base-fee",
                Map.of(),
                "network-maintenance-component", "commission-component"
        )
        and:
        Money result = facade.calculateComponent("base-fee", Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(45), "PLN")

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("base-fee", Parameters.empty())
        assertThat(breakdown)
                .hasName("base-fee")
                .hasTotal(Money.of(BigDecimal.valueOf(45), "PLN"))
                .hasChildrenCount(2)
    }
    def "shouldCalculateMonthlyBillWithDataOverage"() {
        given:
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance")
        facade.createSimpleComponent("commission-component", "commission")
        facade.createSimpleComponent("data-overage-component", "data-overage")

        facade.createCompositeComponent(
                "base-fee",
                Map.of(),
                "network-maintenance-component", "commission-component"
        )

        facade.createCompositeComponent(
                "monthly-bill",
                Map.of(),
                "base-fee", "data-overage-component"
        )
        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3))
        Money result = facade.calculateComponent("monthly-bill", usageParams)
        and:
        assert result == Money.of(BigDecimal.valueOf(51), "PLN")

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("monthly-bill", usageParams)
        assertThat(breakdown)
                .hasName("monthly-bill")
                .hasTotal(Money.of(BigDecimal.valueOf(51), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("base-fee")
                .hasTotal(Money.of(BigDecimal.valueOf(45), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("data-overage-component")
                .hasTotal(Money.of(BigDecimal.valueOf(6), "PLN"))
                .hasNoChildren()
    }
    def "shouldCalculateMonthlyBillWithRoamingOverage"() {
        given:
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance")
        facade.createSimpleComponent("commission-component", "commission")
        facade.createSimpleComponent("roaming-overage-component", "roaming-overage")

        facade.createCompositeComponent(
                "base-fee",
                Map.of(),
                "network-maintenance-component", "commission-component"
        )

        facade.createCompositeComponent(
                "monthly-bill",
                Map.of(),
                "base-fee", "roaming-overage-component"
        )
        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(20))
        Money result = facade.calculateComponent("monthly-bill", usageParams)
        and:
        assert result == Money.of(BigDecimal.valueOf(75), "PLN")
    }
    def "shouldCalculateBillWithVATDependingOnNetAmount"() {
        given:
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance")
        facade.createSimpleComponent("commission-component", "commission")
        facade.createSimpleComponent("data-overage-component", "data-overage")

        facade.createCompositeComponent(
                "base-fee",
                Map.of(),
                "network-maintenance-component", "commission-component"
        )

        facade.createCompositeComponent(
                "net-amount",
                Map.of(),
                "base-fee", "data-overage-component"
        )
        facade.createSimpleComponent("vat-component", "percentage-rate")
        facade.createCompositeComponent(
                "total-bill",
                Map.of(
                        "vat-component", Map.of(
                                "baseAmount", new ValueOf("net-amount")
                        )
                ),
                "net-amount", "vat-component"
        )
        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3))
        Money result = facade.calculateComponent("total-bill", usageParams)
        and:
        Money expectedNet = Money.of(BigDecimal.valueOf(51), "PLN")
        Money expectedVAT = Money.of(new BigDecimal("11.73"), "PLN")
        Money expectedTotal = Money.of(new BigDecimal("62.73"), "PLN")

        assert result == expectedTotal
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-bill", usageParams)
        assertThat(breakdown)
                .hasName("total-bill")
                .hasTotal(expectedTotal)
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("net-amount")
                .hasTotal(expectedNet)
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("vat-component")
                .hasTotal(expectedVAT)
                .hasNoChildren()
    }
    def "shouldShowDetailedBreakdownHierarchy"() {
        given:
        facade.createSimpleComponent("network-maintenance-component", "network-maintenance")
        facade.createSimpleComponent("commission-component", "commission")
        facade.createSimpleComponent("data-overage-component", "data-overage")

        facade.createCompositeComponent(
                "base-fee",
                Map.of(),
                "network-maintenance-component", "commission-component"
        )

        facade.createCompositeComponent(
                "monthly-bill",
                Map.of(),
                "base-fee", "data-overage-component"
        )
        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3))
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("monthly-bill", usageParams)
        and:
        assertThat(breakdown)
                .hasName("monthly-bill")
                .hasTotal(Money.of(BigDecimal.valueOf(51), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("base-fee")
                .hasName("base-fee")
                .hasTotal(Money.of(BigDecimal.valueOf(45), "PLN"))
                .hasChildrenCount(2)
                .child("network-maintenance-component")
                .hasTotal(Money.of(BigDecimal.valueOf(25), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("base-fee")
                .child("commission-component")
                .hasTotal(Money.of(BigDecimal.valueOf(20), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("data-overage-component")
                .hasTotal(Money.of(BigDecimal.valueOf(6), "PLN"))
                .hasNoChildren()
    }
}

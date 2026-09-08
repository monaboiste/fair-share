package com.softwarearchetypes.pricing

import static com.softwarearchetypes.pricing.ApplicabilityConstraint.greaterThanOrEqualTo
import static com.softwarearchetypes.pricing.ComponentBreakdownAssert.assertThat
import static java.time.Clock.fixed

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.List
import java.util.Map
import spock.lang.Specification


class HomeworkSpec extends Specification {

    static final Instant NOW = LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant()
    static final Clock clock = fixed(NOW, ZoneId.systemDefault())

    private PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()
    def setup() {
        LocalDateTime januaryFirst = LocalDateTime.of(2025, 1, 1, 0, 0)
        LocalDateTime aprilFirst = LocalDateTime.of(2025, 4, 1, 0, 0)
        Calculator lightRate = facade.addCalculator("base-rate-light", CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(new BigDecimal("7.90"), "PLN"),
                        "interpretation", Interpretation.UNIT))
        Calculator mediumRate = facade.addCalculator("base-rate-medium", CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(new BigDecimal("6.10"), "PLN"),
                        "interpretation", Interpretation.UNIT))
        Calculator heavyRate = facade.addCalculator("base-rate-heavy", CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(new BigDecimal("5.20"), "PLN"),
                        "interpretation", Interpretation.UNIT))
        facade.addCalculator("base-by-weight", CalculatorType.COMPOSITE,
                Parameters.of(
                        "ranges", List.of(
                                CalculatorRange.numeric(
                                        new BigDecimal("1"), new BigDecimal("5"), lightRate.getId()),
                                CalculatorRange.numeric(
                                        new BigDecimal("5"), new BigDecimal("30"), mediumRate.getId()),
                                CalculatorRange.numeric(
                                        new BigDecimal("30"), new BigDecimal("70"), heavyRate.getId())),
                        "rangeSelector", "quantity"))

        facade.addCalculator("fuel-rate-4.5", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("4.5")))
        facade.addCalculator("fuel-rate-5.0", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("5.0")))
        facade.addCalculator("adr-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("50")))
        facade.addCalculator("oversized-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("35")))
        facade.addCalculator("time-window-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("25")))
        facade.addCalculator("cod-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("2")))
        facade.addCalculator("insurance-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("0.15")))
        facade.addCalculator("vat-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", new BigDecimal("23")))

        facade.createSimpleComponent(
                "base-component",
                "base-by-weight",
                Map.of("weight", "quantity"),
                Validity.from(januaryFirst))
        facade.createSimpleComponent(
                "fuel-component",
                "fuel-rate-4.5",
                Map.of(),
                Validity.between(januaryFirst, aprilFirst))
        facade.createSimpleComponent(
                "fuel-component",
                "fuel-rate-5.0",
                Map.of(),
                Validity.from(aprilFirst))
        facade.createSimpleComponent(
                "adr-component",
                "adr-rate",
                Map.of(),
                ApplicabilityConstraint.equalsTo("cargo-type", "hazmat"),
                Validity.from(januaryFirst))
        facade.createSimpleComponent(
                "oversized-component",
                "oversized-rate",
                Map.of(),
                greaterThanOrEqualTo("weight", 30),
                Validity.from(januaryFirst))
        facade.createSimpleComponent(
                "time-window-component",
                "time-window-rate",
                Map.of(),
                ApplicabilityConstraint.equalsTo("delivery-type", "time-window"),
                Validity.from(januaryFirst))

        facade.createSimpleComponent(
                "cod-component",
                "cod-rate",
                Map.of("cod-value", "baseAmount"),
                Validity.from(januaryFirst))
        facade.createSimpleComponent(
                "insurance-component",
                "insurance-rate",
                Map.of("insured-value", "baseAmount"),
                Validity.from(januaryFirst))
        facade.createSimpleComponent(
                "vat-component",
                "vat-rate",
                Map.of(),
                Validity.from(januaryFirst))
        facade.createCompositeComponent(
                "netto",
                Map.of(
                        "fuel-component", Map.of("baseAmount", new ValueOf("base-component")),
                        "adr-component", Map.of("baseAmount", new ValueOf("base-component")),
                        "oversized-component", Map.of("baseAmount", new ValueOf("base-component")),
                        "time-window-component", Map.of("baseAmount", new ValueOf("base-component"))),
                Validity.from(januaryFirst),
                "base-component",
                "fuel-component",
                "adr-component",
                "oversized-component",
                "time-window-component",
                "cod-component",
                "insurance-component")

        facade.createCompositeComponent(
                "total-cost",
                Map.of("vat-component", Map.of("baseAmount", new ValueOf("netto"))),
                Validity.from(januaryFirst),
                "netto",
                "vat-component")
    }
    def "shouldCalculateStandardShipmentWithFuelSurchargeAndVAT"() {
        given:
        Parameters params = Parameters.of(
                "weight",         BigDecimal.valueOf(3),
                "cargo-type",     "standard",
                "delivery-type",  "standard",
                "cod-value",      Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value",  Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        Money result = facade.calculateComponent("total-cost", params)
        assert result == Money.of(new BigDecimal("30.47"), "PLN")

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-cost", params)
        assertThat(breakdown)
                .hasName("total-cost")
                .hasTotal(Money.of(new BigDecimal("30.47"), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("netto")
                .hasTotal(Money.of(new BigDecimal("24.77"), "PLN"))
                .hasChildrenCount(7)

        assertThat(breakdown).child("netto").child("base-component")
                .hasTotal(Money.of(new BigDecimal("23.70"), "PLN")).hasNoChildren()
        assertThat(breakdown).child("netto").child("fuel-component")
                .hasTotal(Money.of(new BigDecimal("1.07"), "PLN")).hasNoChildren()

        assertThat(breakdown).child("vat-component")
                .hasTotal(Money.of(new BigDecimal("5.70"), "PLN")).hasNoChildren()
    }
    def "shouldCalculateHazmatShipmentWithCODAndInsurance"() {
        given:
        Parameters params = Parameters.of(
                "weight",         BigDecimal.valueOf(12),
                "cargo-type",     "hazmat",
                "delivery-type",  "standard",
                "cod-value",      Money.of(BigDecimal.valueOf(800), "PLN"),
                "insured-value",  Money.of(BigDecimal.valueOf(1500), "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        Money result = facade.calculateComponent("total-cost", params)
        assert result == Money.of(new BigDecimal("161.55"), "PLN")

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-cost", params)
        assertThat(breakdown)
                .hasName("total-cost")
                .hasTotal(Money.of(new BigDecimal("161.55"), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("netto")
                .hasTotal(Money.of(new BigDecimal("131.34"), "PLN"))
                .hasChildrenCount(7)

        assertThat(breakdown).child("netto").child("base-component")
                .hasTotal(Money.of(new BigDecimal("73.20"), "PLN"))
        assertThat(breakdown).child("netto").child("fuel-component")
                .hasTotal(Money.of(new BigDecimal("3.29"), "PLN"))
        assertThat(breakdown).child("netto").child("adr-component")
                .hasTotal(Money.of(new BigDecimal("36.60"), "PLN"))
        assertThat(breakdown).child("netto").child("cod-component")
                .hasTotal(Money.of(new BigDecimal("16.00"), "PLN"))
        assertThat(breakdown).child("netto").child("insurance-component")
                .hasTotal(Money.of(new BigDecimal("2.25"), "PLN"))

        assertThat(breakdown).child("vat-component")
                .hasTotal(Money.of(new BigDecimal("30.21"), "PLN"))
    }
    def "shouldCalculateOversizedShipmentWithTimeWindowDelivery"() {
        given:
        Parameters params = Parameters.of(
                "weight",         BigDecimal.valueOf(45),
                "cargo-type",     "standard",
                "delivery-type",  "time-window",
                "cod-value",      Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value",  Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        Money result = facade.calculateComponent("total-cost", params)
        assert result == Money.of(new BigDecimal("473.46"), "PLN")

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-cost", params)
        assertThat(breakdown)
                .hasName("total-cost")
                .hasTotal(Money.of(new BigDecimal("473.46"), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("netto")
                .hasTotal(Money.of(new BigDecimal("384.93"), "PLN"))
                .hasChildrenCount(7)

        assertThat(breakdown).child("netto").child("base-component")
                .hasTotal(Money.of(new BigDecimal("234.00"), "PLN"))
        assertThat(breakdown).child("netto").child("fuel-component")
                .hasTotal(Money.of(new BigDecimal("10.53"), "PLN"))
        assertThat(breakdown).child("netto").child("oversized-component")
                .hasTotal(Money.of(new BigDecimal("81.90"), "PLN"))
        assertThat(breakdown).child("netto").child("time-window-component")
                .hasTotal(Money.of(new BigDecimal("58.50"), "PLN"))

        assertThat(breakdown).child("vat-component")
                .hasTotal(Money.of(new BigDecimal("88.53"), "PLN"))
    }
    def "shouldApplyFuelRateChangeTemporallyFrom1April"() {
        given:

        Parameters jan = Parameters.of(
                "weight",         BigDecimal.valueOf(3),
                "cargo-type",     "standard",
                "delivery-type",  "standard",
                "cod-value",      Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value",  Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        Parameters apr = Parameters.of(
                "weight",         BigDecimal.valueOf(3),
                "cargo-type",     "standard",
                "delivery-type",  "standard",
                "cod-value",      Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value",  Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 4, 15, 10, 0))
        assert facade.calculateComponent("total-cost", jan) == Money.of(new BigDecimal("30.47"), "PLN")
        assert facade.calculateComponent("total-cost", apr) == Money.of(new BigDecimal("30.61"), "PLN")
    }
}

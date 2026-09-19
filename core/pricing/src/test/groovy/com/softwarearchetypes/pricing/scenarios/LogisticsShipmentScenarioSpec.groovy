package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.calculation.CalculatorRange
import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Interpretation
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.component.ApplicabilityConstraint
import com.softwarearchetypes.pricing.component.Component
import com.softwarearchetypes.pricing.component.ComponentBreakdown
import com.softwarearchetypes.pricing.component.ParameterExpression
import com.softwarearchetypes.pricing.component.SimpleComponentVersion
import com.softwarearchetypes.pricing.component.Validity
import com.softwarearchetypes.quantity.money.Money
import java.time.LocalDateTime
import spock.lang.Specification

class LogisticsShipmentScenarioSpec extends Specification {

    private Component totalCost

    def setup() {
        def light = Calculators.fixed("base-rate-light", Money.of(new BigDecimal("7.90"), "PLN"), Interpretation.UNIT)
        def medium = Calculators.fixed("base-rate-medium", Money.of(new BigDecimal("6.10"), "PLN"), Interpretation.UNIT)
        def heavy = Calculators.fixed("base-rate-heavy", Money.of(new BigDecimal("5.20"), "PLN"), Interpretation.UNIT)
        def baseByWeight = Calculators.composite("base-by-weight", "quantity", List.of(
                CalculatorRange.numeric(new BigDecimal("1"), new BigDecimal("5"), light.id()),
                CalculatorRange.numeric(new BigDecimal("5"), new BigDecimal("30"), medium.id()),
                CalculatorRange.numeric(new BigDecimal("30"), new BigDecimal("70"), heavy.id())), [light, medium, heavy])
        def base = Component.simple("base-component", baseByWeight, Map.of("weight", "quantity"), Validity.from(LocalDateTime.of(2025, 1, 1, 0, 0)))
        def fuel = Component.simple("fuel-component", Calculators.percentage("fuel-rate-4.5", new BigDecimal("4.5")), Map.of(), Validity.between(LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 4, 1, 0, 0)))
            .updateWith(new SimpleComponentVersion(Calculators.percentage("fuel-rate-5.0", new BigDecimal("5.0")), Map.of(), Validity.from(LocalDateTime.of(2025, 4, 1, 0, 0)), LocalDateTime.now()))
        def adr = Component.simple("adr-component", Calculators.percentage("adr-rate", new BigDecimal("50")), ApplicabilityConstraint.equalsTo("cargo-type", "hazmat"))
        def oversized = Component.simple("oversized-component", Calculators.percentage("oversized-rate", new BigDecimal("35")), ApplicabilityConstraint.greaterThanOrEqualTo("weight", 30))
        def timeWindow = Component.simple("time-window-component", Calculators.percentage("time-window-rate", new BigDecimal("25")), ApplicabilityConstraint.equalsTo("delivery-type", "time-window"))
        def cod = Component.simple("cod-component", Calculators.percentage("cod-rate", new BigDecimal("2")), Map.of("cod-value", "baseAmount"))
        def insurance = Component.simple("insurance-component", Calculators.percentage("insurance-rate", new BigDecimal("0.15")), Map.of("insured-value", "baseAmount"))
        def vat = Component.simple("vat-component", Calculators.percentage("vat-rate", new BigDecimal("23")))
        def netto = Component.composite("netto", [
                "fuel-component": Map.of("baseAmount", ParameterExpression.valueOf("base-component")),
                "adr-component": Map.of("baseAmount", ParameterExpression.valueOf("base-component")),
                "oversized-component": Map.of("baseAmount", ParameterExpression.valueOf("base-component")),
                "time-window-component": Map.of("baseAmount", ParameterExpression.valueOf("base-component"))],
                base, fuel, adr, oversized, timeWindow, cod, insurance)
        totalCost = Component.composite("total-cost", Map.of("vat-component", Map.<String, ParameterExpression>of("baseAmount", ParameterExpression.valueOf("netto"))), netto, vat)
    }

    def "standard 3 kg shipment includes fuel surcharge and VAT"() {
        given:
        Parameters params = Parameters.of(
                "weight", BigDecimal.valueOf(3),
                "cargo-type", "standard",
                "delivery-type", "standard",
                "cod-value", Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value", Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        and:
        Money result = totalCost.calculate(params).money()

        expect:
        result == Money.of(new BigDecimal("30.47"), "PLN")

        ComponentBreakdown breakdown = totalCost.calculateBreakdown(params)
        breakdown.name() == "total-cost"
        breakdown.total() == Money.of(new BigDecimal("30.47"), "PLN")

        def components = breakdown.children()
        components.size() == 2

        components.find { it.name() == "netto" }.total() == Money.of(new BigDecimal("24.77"), "PLN")
        components.find { it.name() == "netto" }.children().size() == 7

        components.find { it.name() == "netto" }.children().find { it.name() == "base-component" }.total() == Money.of(new BigDecimal("23.70"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "base-component" }.children().isEmpty()
        components.find { it.name() == "netto" }.children().find { it.name() == "fuel-component" }.total() == Money.of(new BigDecimal("1.07"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "fuel-component" }.children().isEmpty()

        components.find { it.name() == "vat-component" }.total() == Money.of(new BigDecimal("5.70"), "PLN")
        components.find { it.name() == "vat-component" }.children().isEmpty()
    }

    def "12 kg hazmat shipment with COD and insurance adds all applicable surcharges"() {
        given:
        Parameters params = Parameters.of(
                "weight", BigDecimal.valueOf(12),
                "cargo-type", "hazmat",
                "delivery-type", "standard",
                "cod-value", Money.of(BigDecimal.valueOf(800), "PLN"),
                "insured-value", Money.of(BigDecimal.valueOf(1500), "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        and:
        Money result = totalCost.calculate(params).money()

        expect:
        result == Money.of(new BigDecimal("161.55"), "PLN")

        ComponentBreakdown breakdown = totalCost.calculateBreakdown(params)
        breakdown.name() == "total-cost"
        breakdown.total() == Money.of(new BigDecimal("161.55"), "PLN")

        def components = breakdown.children()
        components.size() == 2

        components.find { it.name() == "netto" }.total() == Money.of(new BigDecimal("131.34"), "PLN")
        components.find { it.name() == "netto" }.children().size() == 7

        components.find { it.name() == "netto" }.children().find { it.name() == "base-component" }.total() == Money.of(new BigDecimal("73.20"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "fuel-component" }.total() == Money.of(new BigDecimal("3.29"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "adr-component" }.total() == Money.of(new BigDecimal("36.60"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "cod-component" }.total() == Money.of(new BigDecimal("16.00"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "insurance-component" }.total() == Money.of(new BigDecimal("2.25"), "PLN")

        components.find { it.name() == "vat-component" }.total() == Money.of(new BigDecimal("30.21"), "PLN")
    }

    def "45 kg oversized shipment with time-window delivery applies both surcharges"() {
        given:
        Parameters params = Parameters.of(
                "weight", BigDecimal.valueOf(45),
                "cargo-type", "standard",
                "delivery-type", "time-window",
                "cod-value", Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value", Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        and:
        Money result = totalCost.calculate(params).money()

        expect:
        result == Money.of(new BigDecimal("473.46"), "PLN")

        ComponentBreakdown breakdown = totalCost.calculateBreakdown(params)
        breakdown.name() == "total-cost"
        breakdown.total() == Money.of(new BigDecimal("473.46"), "PLN")
        def components = breakdown.children()
        components.size() == 2

        components.find { it.name() == "netto" }.total() == Money.of(new BigDecimal("384.93"), "PLN")
        components.find { it.name() == "netto" }.children().size() == 7

        components.find { it.name() == "netto" }.children().find { it.name() == "base-component" }.total() == Money.of(new BigDecimal("234.00"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "fuel-component" }.total() == Money.of(new BigDecimal("10.53"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "oversized-component" }.total() == Money.of(new BigDecimal("81.90"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "time-window-component" }.total() == Money.of(new BigDecimal("58.50"), "PLN")

        components.find { it.name() == "vat-component" }.total() == Money.of(new BigDecimal("88.53"), "PLN")
    }

    def "fuel surcharge rate changes from 4.5% to 5.0% on 1 April 2025"() {
        given:
        Parameters jan = Parameters.of(
                "weight", BigDecimal.valueOf(3),
                "cargo-type", "standard",
                "delivery-type", "standard",
                "cod-value", Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value", Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 1, 20, 10, 0))

        Parameters apr = Parameters.of(
                "weight", BigDecimal.valueOf(3),
                "cargo-type", "standard",
                "delivery-type", "standard",
                "cod-value", Money.of(BigDecimal.ZERO, "PLN"),
                "insured-value", Money.of(BigDecimal.ZERO, "PLN"))
                .with("timestamp", LocalDateTime.of(2025, 4, 15, 10, 0))

        expect:
        totalCost.calculate(jan).money() == Money.of(new BigDecimal("30.47"), "PLN")
        totalCost.calculate(apr).money() == Money.of(new BigDecimal("30.61"), "PLN")
    }
}

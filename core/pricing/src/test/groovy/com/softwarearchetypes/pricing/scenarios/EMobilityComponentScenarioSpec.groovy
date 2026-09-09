package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.CalculatorType
import com.softwarearchetypes.pricing.ComponentBreakdown
import com.softwarearchetypes.pricing.Interpretation
import com.softwarearchetypes.pricing.ParameterValue
import com.softwarearchetypes.pricing.Parameters
import com.softwarearchetypes.pricing.PricingFacade
import com.softwarearchetypes.pricing.PricingTestConfiguration
import com.softwarearchetypes.pricing.StepBoundary
import com.softwarearchetypes.pricing.ValueOf
import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class EMobilityComponentScenarioSpec extends Specification {

    private final PricingFacade facade = PricingTestConfiguration.inMemory(Clock.systemUTC())

    def setup() {
        facade.addCalculator("energy-wholesale", CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(BigDecimal.valueOf(0.60), "PLN"),
                        "stepSize", BigDecimal.valueOf(5),
                        "stepIncrement", BigDecimal.valueOf(0.10),
                        "stepBoundary", StepBoundary.INCLUSIVE,
                        "interpretation", Interpretation.MARGINAL
                ))
        facade.addCalculator("energy-grid", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(0.15), "PLN"),
                        "interpretation", Interpretation.UNIT
                ))
        facade.addCalculator("cpo-session-fee", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(new BigDecimal("1.50"), "PLN"),
                        "interpretation", Interpretation.TOTAL))
        facade.addCalculator("cpo-per-kwh", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(0.25), "PLN"),
                        "interpretation", Interpretation.UNIT
                ))
        facade.addCalculator("cpo-per-minute", CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(BigDecimal.valueOf(0.10), "PLN"),
                        "interpretation", Interpretation.UNIT
                ))
        facade.addCalculator("emsp-per-kwh", CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(BigDecimal.valueOf(0.10), "PLN"),
                        "interpretation", Interpretation.UNIT
                ))
        facade.addCalculator("emsp-per-minute", CalculatorType.SIMPLE_FIXED,
                Parameters.of(
                        "amount", Money.of(BigDecimal.valueOf(0.05), "PLN"),
                        "interpretation", Interpretation.UNIT
                ))
        facade.addCalculator("vat-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(23),
                        "interpretation", Interpretation.TOTAL))
    }

    def "complete EV charging session is calculated with a full cost breakdown"() {
        given: "a 12 kWh, 40-minute charging session with energy, CPO, EMSP, and VAT components"
        facade.createSimpleComponent("energy-wholesale-component", "energy-wholesale")
        facade.createSimpleComponent("energy-grid-component", "energy-grid")
        facade.createCompositeComponent("energy-net",
                Map.of(),
                "energy-wholesale-component", "energy-grid-component"
        )
        facade.createSimpleComponent("cpo-session-component", "cpo-session-fee")
        facade.createSimpleComponent("cpo-kwh-component", "cpo-per-kwh")
        facade.createSimpleComponent("cpo-time-component", "cpo-per-minute", Map.of("time", "quantity"))

        facade.createCompositeComponent("cpo-markup",
                Map.of(),
                "cpo-session-component", "cpo-kwh-component", "cpo-time-component"
        )
        facade.createSimpleComponent("emsp-kwh-component", "emsp-per-kwh")
        facade.createSimpleComponent("emsp-time-component", "emsp-per-minute", Map.of("time", "quantity"))

        facade.createCompositeComponent("emsp-markup",
                Map.of(),
                "emsp-kwh-component", "emsp-time-component"
        )
        facade.createCompositeComponent("netto",
                Map.of(),
                "energy-net", "cpo-markup", "emsp-markup"
        )
        facade.createSimpleComponent("vat-component", "vat-rate")

        Map<String, Map<String, ParameterValue>> dependencies = Map.<String, Map<String, ParameterValue>> of(
                "vat-component", Map.of("baseAmount", new ValueOf("netto")))
        facade.createCompositeComponent("total-session-cost", dependencies,
                "netto", "vat-component"
        )

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        Money result = facade.calculateComponent("total-session-cost", sessionParams)

        Money expectedTotal = Money.of(new BigDecimal("26.57"), "PLN")

        expect:
        result == expectedTotal
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-session-cost", sessionParams)

        breakdown.name() == "total-session-cost"
        breakdown.total() == Money.of(new BigDecimal("26.57"), "PLN")
        breakdown.children().size() == 2
        breakdown.children().find { it.name() == "netto" }.total() == Money.of(new BigDecimal("21.60"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().size() == 3
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.total() == Money.of(new BigDecimal("9.90"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().size() == 2
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().find { it.name() == "energy-wholesale-component" }.total() == Money.of(new BigDecimal("8.10"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().find { it.name() == "energy-wholesale-component" }.children().isEmpty()

        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().find { it.name() == "energy-grid-component" }.total() == Money.of(new BigDecimal("1.80"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.total() == Money.of(new BigDecimal("8.50"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().size() == 3
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-session-component" }.total() == Money.of(new BigDecimal("1.50"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-session-component" }.children().isEmpty()

        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-kwh-component" }.total() == Money.of(new BigDecimal("3.00"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-kwh-component" }.children().isEmpty()

        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-time-component" }.total() == Money.of(new BigDecimal("4.00"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-time-component" }.children().isEmpty()
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.total() == Money.of(new BigDecimal("3.20"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().size() == 2
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-kwh-component" }.total() == Money.of(new BigDecimal("1.20"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-kwh-component" }.children().isEmpty()

        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-time-component" }.total() == Money.of(new BigDecimal("2.00"), "PLN")
        breakdown.children().find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-time-component" }.children().isEmpty()
        breakdown.children().find { it.name() == "vat-component" }.total() == Money.of(new BigDecimal("4.97"), "PLN")
    }

    def "charging session breakdown can be formatted for display"() {
        given: "a fully configured charging session"
        facade.createSimpleComponent("energy-wholesale-component", "energy-wholesale")
        facade.createSimpleComponent("energy-grid-component", "energy-grid")
        facade.createCompositeComponent("energy-net", Map.of(),
                "energy-wholesale-component", "energy-grid-component")

        facade.createSimpleComponent("cpo-session-component", "cpo-session-fee")
        facade.createSimpleComponent("cpo-kwh-component", "cpo-per-kwh")
        facade.createSimpleComponent("cpo-time-component", "cpo-per-minute", Map.of("time", "quantity"))
        facade.createCompositeComponent("cpo-markup", Map.of(),
                "cpo-session-component", "cpo-kwh-component", "cpo-time-component")

        facade.createSimpleComponent("emsp-kwh-component", "emsp-per-kwh")
        facade.createSimpleComponent("emsp-time-component", "emsp-per-minute", Map.of("time", "quantity"))
        facade.createCompositeComponent("emsp-markup", Map.of(),
                "emsp-kwh-component", "emsp-time-component")

        facade.createCompositeComponent("netto", Map.of(),
                "energy-net", "cpo-markup", "emsp-markup")

        facade.createSimpleComponent("vat-component", "vat-rate")
        Map<String, Map<String, ParameterValue>> dependencies = Map.<String, Map<String, ParameterValue>> of(
                "vat-component", Map.of("baseAmount", new ValueOf("netto")))
        facade.createCompositeComponent("total-session-cost", dependencies,
                "netto", "vat-component")

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        and:
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-session-cost", sessionParams)

        expect:
        breakdown.total() == Money.of(new BigDecimal("26.57"), "PLN")
        breakdown.format() != null
        breakdown.format().contains("total-session-cost")
        breakdown.format().contains("netto")
    }
}

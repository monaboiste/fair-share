package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.StepBoundary
import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Interpretation
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.calculation.StepBoundary
import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class EMobilityComponentScenarioSpec extends Specification {


    private Calculator vatrate
    private Calculator emspperminute
    private Calculator emspperkwh
    private Calculator cpoperminute
    private Calculator cpoperkwh
    private Calculator cposessionfee
    private Calculator energygrid
    private Calculator energywholesale

    def setup() {
        energywholesale = Calculators.stepFunction("energy-wholesale", Money.of(BigDecimal.valueOf(0.60), "PLN"), BigDecimal.valueOf(5), BigDecimal.valueOf(0.10), Interpretation.MARGINAL, StepBoundary.INCLUSIVE)
        energygrid = Calculators.fixed("energy-grid", Money.of(BigDecimal.valueOf(0.15), "PLN"), Interpretation.UNIT)
        cposessionfee = Calculators.fixed("cpo-session-fee", Money.of(new BigDecimal("1.50"), "PLN"), Interpretation.TOTAL)
        cpoperkwh = Calculators.fixed("cpo-per-kwh", Money.of(BigDecimal.valueOf(0.25), "PLN"), Interpretation.UNIT)
        cpoperminute = Calculators.fixed("cpo-per-minute", Money.of(BigDecimal.valueOf(0.10), "PLN"), Interpretation.UNIT)
        emspperkwh = Calculators.fixed("emsp-per-kwh", Money.of(BigDecimal.valueOf(0.10), "PLN"), Interpretation.UNIT)
        emspperminute = Calculators.fixed("emsp-per-minute", Money.of(BigDecimal.valueOf(0.05), "PLN"), Interpretation.UNIT)
        vatrate = Calculators.percentage("vat-rate", BigDecimal.valueOf(23))
    }

    def "complete EV charging session is calculated with a full cost breakdown"() {
        given: "a 12 kWh, 40-minute charging session with energy, CPO, EMSP, and VAT components"
        def energy_wholesale_component = Component.simple("energy-wholesale-component", energywholesale)
        def energy_grid_component = Component.simple("energy-grid-component", energygrid)
        def energy_net = Component.composite("energy-net", Map.of(), energy_wholesale_component, energy_grid_component)
        def cpo_session_component = Component.simple("cpo-session-component", cposessionfee)
        def cpo_kwh_component = Component.simple("cpo-kwh-component", cpoperkwh)
        def cpo_time_component = Component.simple("cpo-time-component", cpoperminute, Map.of("time", "quantity"))

        def cpo_markup = Component.composite("cpo-markup", Map.of(), cpo_session_component, cpo_kwh_component, cpo_time_component)
        def emsp_kwh_component = Component.simple("emsp-kwh-component", emspperkwh)
        def emsp_time_component = Component.simple("emsp-time-component", emspperminute, Map.of("time", "quantity"))

        def emsp_markup = Component.composite("emsp-markup", Map.of(), emsp_kwh_component, emsp_time_component)
        def netto = Component.composite("netto", Map.of(), energy_net, cpo_markup, emsp_markup)
        def vat_component = Component.simple("vat-component", vatrate)

        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "vat-component", Map.of("baseAmount", ParameterExpression.valueOf("netto")))
        def total_session_cost = Component.composite("total-session-cost", dependencies, netto, vat_component)

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        Money result = total_session_cost.calculate( sessionParams)

        Money expectedTotal = Money.of(new BigDecimal("26.57"), "PLN")

        expect:
        result == expectedTotal
        ComponentBreakdown breakdown = total_session_cost.calculateBreakdown( sessionParams)

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
        def energy_wholesale_component = Component.simple("energy-wholesale-component", energywholesale)
        def energy_grid_component = Component.simple("energy-grid-component", energygrid)
        def energy_net = Component.composite("energy-net", Map.of(), energy_wholesale_component, energy_grid_component)

        def cpo_session_component = Component.simple("cpo-session-component", cposessionfee)
        def cpo_kwh_component = Component.simple("cpo-kwh-component", cpoperkwh)
        def cpo_time_component = Component.simple("cpo-time-component", cpoperminute, Map.of("time", "quantity"))
        def cpo_markup = Component.composite("cpo-markup", Map.of(), cpo_session_component, cpo_kwh_component, cpo_time_component)

        def emsp_kwh_component = Component.simple("emsp-kwh-component", emspperkwh)
        def emsp_time_component = Component.simple("emsp-time-component", emspperminute, Map.of("time", "quantity"))
        def emsp_markup = Component.composite("emsp-markup", Map.of(), emsp_kwh_component, emsp_time_component)

        def netto = Component.composite("netto", Map.of(), energy_net, cpo_markup, emsp_markup)

        def vat_component = Component.simple("vat-component", vatrate)
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "vat-component", Map.of("baseAmount", ParameterExpression.valueOf("netto")))
        def total_session_cost = Component.composite("total-session-cost", dependencies, netto, vat_component)

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        and:
        ComponentBreakdown breakdown = total_session_cost.calculateBreakdown( sessionParams)

        expect:
        breakdown.total() == Money.of(new BigDecimal("26.57"), "PLN")
        breakdown.format() != null
        breakdown.format().contains("total-session-cost")
        breakdown.format().contains("netto")
    }
}

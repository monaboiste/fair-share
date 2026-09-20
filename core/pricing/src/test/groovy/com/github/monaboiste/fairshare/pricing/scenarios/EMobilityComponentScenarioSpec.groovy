package com.github.monaboiste.fairshare.pricing.scenarios

import com.github.monaboiste.fairshare.pricing.calculation.Calculator
import com.github.monaboiste.fairshare.pricing.calculation.Calculators
import com.github.monaboiste.fairshare.pricing.calculation.Interpretation
import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import com.github.monaboiste.fairshare.pricing.calculation.StepBoundary
import com.github.monaboiste.fairshare.pricing.component.Component
import com.github.monaboiste.fairshare.pricing.component.ComponentBreakdown
import com.github.monaboiste.fairshare.pricing.component.ParameterExpression
import com.github.monaboiste.fairshare.quantity.money.Money
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
        def energyWholesale = Component.simple("energy-wholesale-component", energywholesale)
        def energyGrid = Component.simple("energy-grid-component", energygrid)
        def energyNet = Component.composite("energy-net", Map.of(), energyWholesale, energyGrid)
        def cpoSession = Component.simple("cpo-session-component", cposessionfee)
        def cpoKwh = Component.simple("cpo-kwh-component", cpoperkwh)
        def cpoTime = Component.simple("cpo-time-component", cpoperminute, Map.of("time", "quantity"))

        def cpoMarkup = Component.composite("cpo-markup", Map.of(), cpoSession, cpoKwh, cpoTime)
        def emspKwh = Component.simple("emsp-kwh-component", emspperkwh)
        def emspTime = Component.simple("emsp-time-component", emspperminute, Map.of("time", "quantity"))

        def emspMarkup = Component.composite("emsp-markup", Map.of(), emspKwh, emspTime)
        def netto = Component.composite("netto", Map.of(), energyNet, cpoMarkup, emspMarkup)
        def vat = Component.simple("vat-component", vatrate)

        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "vat-component", Map.of("baseAmount", ParameterExpression.valueOf("netto")))
        def totalSessionCost = Component.composite("total-session-cost", dependencies, netto, vat)

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        Money result = totalSessionCost.calculate(sessionParams).money()

        Money expectedTotal = Money.of(new BigDecimal("26.57"), "PLN")

        expect:
        result == expectedTotal
        ComponentBreakdown breakdown = totalSessionCost.calculateBreakdown(sessionParams)

        breakdown.name() == "total-session-cost"
        breakdown.total() == Money.of(new BigDecimal("26.57"), "PLN")

        def components = breakdown.children()
        components.size() == 2
        components.find { it.name() == "netto" }.total() == Money.of(new BigDecimal("21.60"), "PLN")

        components.find { it.name() == "netto" }.children().size() == 3
        components.find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.total() == Money.of(new BigDecimal("9.90"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().size() == 2
        components.find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().find { it.name() == "energy-wholesale-component" }.total() == Money.of(new BigDecimal("8.10"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().find { it.name() == "energy-wholesale-component" }.children().isEmpty()

        components.find { it.name() == "netto" }.children().find { it.name() == "energy-net" }.children().find { it.name() == "energy-grid-component" }.total() == Money.of(new BigDecimal("1.80"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.total() == Money.of(new BigDecimal("8.50"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().size() == 3
        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-session-component" }.total() == Money.of(new BigDecimal("1.50"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-session-component" }.children().isEmpty()

        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-kwh-component" }.total() == Money.of(new BigDecimal("3.00"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-kwh-component" }.children().isEmpty()

        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-time-component" }.total() == Money.of(new BigDecimal("4.00"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "cpo-markup" }.children().find { it.name() == "cpo-time-component" }.children().isEmpty()
        components.find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.total() == Money.of(new BigDecimal("3.20"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().size() == 2
        components.find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-kwh-component" }.total() == Money.of(new BigDecimal("1.20"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-kwh-component" }.children().isEmpty()

        components.find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-time-component" }.total() == Money.of(new BigDecimal("2.00"), "PLN")
        components.find { it.name() == "netto" }.children().find { it.name() == "emsp-markup" }.children().find { it.name() == "emsp-time-component" }.children().isEmpty()
        components.find { it.name() == "vat-component" }.total() == Money.of(new BigDecimal("4.97"), "PLN")
    }

    def "charging session breakdown can be formatted for display"() {
        given: "a fully configured charging session"
        def energyWholesale = Component.simple("energy-wholesale-component", energywholesale)
        def energyGrid = Component.simple("energy-grid-component", energygrid)
        def energyNet = Component.composite("energy-net", Map.of(), energyWholesale, energyGrid)

        def cpoSession = Component.simple("cpo-session-component", cposessionfee)
        def cpoKwh = Component.simple("cpo-kwh-component", cpoperkwh)
        def cpoTime = Component.simple("cpo-time-component", cpoperminute, Map.of("time", "quantity"))
        def cpoMarkup = Component.composite("cpo-markup", Map.of(), cpoSession, cpoKwh, cpoTime)

        def emspKwh = Component.simple("emsp-kwh-component", emspperkwh)
        def emspTime = Component.simple("emsp-time-component", emspperminute, Map.of("time", "quantity"))
        def emspMarkup = Component.composite("emsp-markup", Map.of(), emspKwh, emspTime)

        def netto = Component.composite("netto", Map.of(), energyNet, cpoMarkup, emspMarkup)

        def vat = Component.simple("vat-component", vatrate)
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "vat-component", Map.of("baseAmount", ParameterExpression.valueOf("netto")))
        def totalSessionCost = Component.composite("total-session-cost", dependencies, netto, vat)

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        and:
        ComponentBreakdown breakdown = totalSessionCost.calculateBreakdown(sessionParams)

        expect:
        breakdown.total() == Money.of(new BigDecimal("26.57"), "PLN")
        breakdown.format() != null
        breakdown.format().contains("total-session-cost")
        breakdown.format().contains("netto")
    }
}

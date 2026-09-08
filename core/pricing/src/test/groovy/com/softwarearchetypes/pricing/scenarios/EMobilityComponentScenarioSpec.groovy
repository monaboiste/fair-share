package com.softwarearchetypes.pricing

import static com.softwarearchetypes.pricing.ComponentBreakdownAssert.assertThat
import static java.time.Clock.fixed

import com.softwarearchetypes.pricing.CalculatorType
import com.softwarearchetypes.pricing.ComponentBreakdown
import com.softwarearchetypes.pricing.Interpretation
import com.softwarearchetypes.pricing.Parameters
import com.softwarearchetypes.pricing.PricingConfiguration
import com.softwarearchetypes.pricing.PricingFacade
import com.softwarearchetypes.pricing.StepBoundary
import com.softwarearchetypes.pricing.ValueOf
import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Map
import spock.lang.Specification


class EMobilityComponentScenarioSpec extends Specification {

    static final Instant NOW = LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant()
    static final Clock clock = fixed(NOW, ZoneId.systemDefault())
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()
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

        facade.createCompositeComponent("total-session-cost",
                Map.of(
                        "vat-component", Map.of(
                                "baseAmount", new ValueOf("netto")
                        )
                ),
                "netto", "vat-component"
        )

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )

        Money result = facade.calculateComponent("total-session-cost", sessionParams)

        Money expectedTotal = Money.of(new BigDecimal("26.57"), "PLN")
        assert result == expectedTotal
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-session-cost", sessionParams)

        assertThat(breakdown)
                .hasName("total-session-cost")
                .hasTotal(Money.of(new BigDecimal("26.57"), "PLN"))
                .hasChildrenCount(2)
        assertThat(breakdown)
                .child("netto")
                .hasTotal(Money.of(new BigDecimal("21.60"), "PLN"))
                .hasChildrenCount(3)
        assertThat(breakdown)
                .child("netto")
                .child("energy-net")
                .hasTotal(Money.of(new BigDecimal("9.90"), "PLN"))
                .hasChildrenCount(2)
        assertThat(breakdown)
                .child("netto")
                .child("energy-net")
                .child("energy-wholesale-component")
                .hasTotal(Money.of(new BigDecimal("8.10"), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("netto")
                .child("energy-net")
                .child("energy-grid-component")
                .hasTotal(Money.of(new BigDecimal("1.80"), "PLN"))
        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .hasTotal(Money.of(new BigDecimal("8.50"), "PLN"))
                .hasChildrenCount(3)
        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .child("cpo-session-component")
                .hasTotal(Money.of(new BigDecimal("1.50"), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .child("cpo-kwh-component")
                .hasTotal(Money.of(new BigDecimal("3.00"), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("netto")
                .child("cpo-markup")
                .child("cpo-time-component")
                .hasTotal(Money.of(new BigDecimal("4.00"), "PLN"))
                .hasNoChildren()
        assertThat(breakdown)
                .child("netto")
                .child("emsp-markup")
                .hasTotal(Money.of(new BigDecimal("3.20"), "PLN"))
                .hasChildrenCount(2)
        assertThat(breakdown)
                .child("netto")
                .child("emsp-markup")
                .child("emsp-kwh-component")
                .hasTotal(Money.of(new BigDecimal("1.20"), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("netto")
                .child("emsp-markup")
                .child("emsp-time-component")
                .hasTotal(Money.of(new BigDecimal("2.00"), "PLN"))
                .hasNoChildren()
        assertThat(breakdown)
                .child("vat-component")
                .hasTotal(Money.of(new BigDecimal("4.97"), "PLN"))
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
        facade.createCompositeComponent("total-session-cost",
                Map.of("vat-component", Map.of("baseAmount", new ValueOf("netto"))),
                "netto", "vat-component")

        Parameters sessionParams = Parameters.of(
                "quantity", BigDecimal.valueOf(12),
                "time", BigDecimal.valueOf(40)
        )
        and:
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-session-cost", sessionParams)
        and:
        assert breakdown.total() == Money.of(new BigDecimal("26.57"), "PLN")
        assert breakdown.format() != null
        assert breakdown.format().contains("total-session-cost")
        assert breakdown.format().contains("netto")
    }
}

package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class TelcoComponentScenarioSpec extends Specification {


    private Calculator percentagerate
    private Calculator roamingoverage
    private Calculator dataoverage
    private Calculator commission
    private Calculator networkmaintenance

    def setup() {
        networkmaintenance = Calculators.fixed("network-maintenance", Money.of(BigDecimal.valueOf(25), "PLN"))

        commission = Calculators.fixed("commission", Money.of(BigDecimal.valueOf(20), "PLN"))

        dataoverage = Calculators.stepFunction("data-overage", Money.of(BigDecimal.ZERO, "PLN"), BigDecimal.ONE, BigDecimal.valueOf(2))

        roamingoverage = Calculators.stepFunction("roaming-overage", Money.of(BigDecimal.ZERO, "PLN"), BigDecimal.ONE, BigDecimal.valueOf(1.5))

        percentagerate = Calculators.percentage("percentage-rate", BigDecimal.valueOf(23))
    }

    def "monthly bill with base fees only totals the sum of included fees"() {
        given: "a base fee composite with network maintenance (25 PLN) and commission (20 PLN)"
        def network_maintenance_component = Component.simple("network-maintenance-component", networkmaintenance)
        def commission_component = Component.simple("commission-component", commission)

        def base_fee = Component.composite("base-fee", Map.of(), network_maintenance_component, commission_component)

        and:
        Money result = base_fee.calculate( Parameters.empty())

        expect:
        result == Money.of(BigDecimal.valueOf(45), "PLN")

        ComponentBreakdown breakdown = base_fee.calculateBreakdown( Parameters.empty())
        breakdown.name() == "base-fee"
        breakdown.total() == Money.of(BigDecimal.valueOf(45), "PLN")
        breakdown.children().size() == 2
    }

    def "monthly bill with data overage includes usage charges"() {
        given: "a monthly bill with base fee and data overage at 2 PLN per MB"
        def network_maintenance_component = Component.simple("network-maintenance-component", networkmaintenance)
        def commission_component = Component.simple("commission-component", commission)
        def data_overage_component = Component.simple("data-overage-component", dataoverage)

        def base_fee = Component.composite("base-fee", Map.of(), network_maintenance_component, commission_component)

        def monthly_bill = Component.composite("monthly-bill", Map.of(), base_fee, data_overage_component)

        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3))
        Money result = monthly_bill.calculate( usageParams)

        expect:
        result == Money.of(BigDecimal.valueOf(51), "PLN")

        ComponentBreakdown breakdown = monthly_bill.calculateBreakdown( usageParams)
        breakdown.name() == "monthly-bill"
        breakdown.total() == Money.of(BigDecimal.valueOf(51), "PLN")
        breakdown.children().size() == 2

        breakdown.children().find { it.name() == "base-fee" }.total() == Money.of(BigDecimal.valueOf(45), "PLN")
        breakdown.children().find { it.name() == "base-fee" }.children().size() == 2

        breakdown.children().find { it.name() == "data-overage-component" }.total() == Money.of(BigDecimal.valueOf(6), "PLN")
        breakdown.children().find { it.name() == "data-overage-component" }.children().isEmpty()
    }

    def "monthly bill with roaming overage includes roaming charges"() {
        given: "a monthly bill with base fee and roaming overage at 1.50 PLN per MB"
        def network_maintenance_component = Component.simple("network-maintenance-component", networkmaintenance)
        def commission_component = Component.simple("commission-component", commission)
        def roaming_overage_component = Component.simple("roaming-overage-component", roamingoverage)

        def base_fee = Component.composite("base-fee", Map.of(), network_maintenance_component, commission_component)

        def monthly_bill = Component.composite("monthly-bill", Map.of(), base_fee, roaming_overage_component)

        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(20))
        Money result = monthly_bill.calculate( usageParams)

        expect:
        result == Money.of(BigDecimal.valueOf(75), "PLN")
    }

    def "total bill includes VAT calculated on the net amount"() {
        given: "a total bill with VAT depending on the net amount via a ValueOf dependency"
        def network_maintenance_component = Component.simple("network-maintenance-component", networkmaintenance)
        def commission_component = Component.simple("commission-component", commission)
        def data_overage_component = Component.simple("data-overage-component", dataoverage)

        def base_fee = Component.composite("base-fee", Map.of(), network_maintenance_component, commission_component)

        def net_amount = Component.composite("net-amount", Map.of(), base_fee, data_overage_component)
        def vat_component = Component.simple("vat-component", percentagerate)
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "vat-component", Map.of("baseAmount", ParameterExpression.valueOf("net-amount")))
        def total_bill = Component.composite("total-bill", dependencies, net_amount, vat_component)

        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3))
        Money result = total_bill.calculate( usageParams)

        and:
        Money expectedNet = Money.of(BigDecimal.valueOf(51), "PLN")
        Money expectedVAT = Money.of(new BigDecimal("11.73"), "PLN")
        Money expectedTotal = Money.of(new BigDecimal("62.73"), "PLN")

        expect:
        result == expectedTotal
        ComponentBreakdown breakdown = total_bill.calculateBreakdown( usageParams)
        breakdown.name() == "total-bill"
        breakdown.total() == expectedTotal
        breakdown.children().size() == 2

        breakdown.children().find { it.name() == "net-amount" }.total() == expectedNet
        breakdown.children().find { it.name() == "net-amount" }.children().size() == 2

        breakdown.children().find { it.name() == "vat-component" }.total() == expectedVAT
        breakdown.children().find { it.name() == "vat-component" }.children().isEmpty()
    }

    def "detailed breakdown shows the full component hierarchy"() {
        given: "a two-level composite with a nested base fee"
        def network_maintenance_component = Component.simple("network-maintenance-component", networkmaintenance)
        def commission_component = Component.simple("commission-component", commission)
        def data_overage_component = Component.simple("data-overage-component", dataoverage)

        def base_fee = Component.composite("base-fee", Map.of(), network_maintenance_component, commission_component)

        def monthly_bill = Component.composite("monthly-bill", Map.of(), base_fee, data_overage_component)

        and:
        Parameters usageParams = Parameters.of("quantity", BigDecimal.valueOf(3))
        ComponentBreakdown breakdown = monthly_bill.calculateBreakdown( usageParams)

        expect:
        breakdown.name() == "monthly-bill"
        breakdown.total() == Money.of(BigDecimal.valueOf(51), "PLN")
        breakdown.children().size() == 2

        breakdown.children().find { it.name() == "base-fee" }.name() == "base-fee"
        breakdown.children().find { it.name() == "base-fee" }.total() == Money.of(BigDecimal.valueOf(45), "PLN")
        breakdown.children().find { it.name() == "base-fee" }.children().size() == 2
        breakdown.children().find { it.name() == "base-fee" }.children().find { it.name() == "network-maintenance-component" }.total() == Money.of(BigDecimal.valueOf(25), "PLN")
        breakdown.children().find { it.name() == "base-fee" }.children().find { it.name() == "network-maintenance-component" }.children().isEmpty()

        breakdown.children().find { it.name() == "base-fee" }.children().find { it.name() == "commission-component" }.total() == Money.of(BigDecimal.valueOf(20), "PLN")
        breakdown.children().find { it.name() == "base-fee" }.children().find { it.name() == "commission-component" }.children().isEmpty()

        breakdown.children().find { it.name() == "data-overage-component" }.total() == Money.of(BigDecimal.valueOf(6), "PLN")
        breakdown.children().find { it.name() == "data-overage-component" }.children().isEmpty()
    }
}

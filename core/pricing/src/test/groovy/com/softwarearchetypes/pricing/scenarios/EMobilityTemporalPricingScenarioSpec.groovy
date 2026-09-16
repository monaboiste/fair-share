package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.component.Component
import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class EMobilityTemporalPricingScenarioSpec extends Specification {
    def "temporal pricing component exposes a breakdown"() {
        given:
        def component = Component.simple("charging", Calculators.fixed("charging", Money.of(12, "PLN")))

        expect:
        component.calculateBreakdown(Parameters.empty()).children().isEmpty()
        component.calculateBreakdown(Parameters.empty()).total() == Money.of(12, "PLN")
    }
}

package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.component.Component
import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class EMobilityComponentScenarioSpec extends Specification {
    def "charging component calculates its configured price"() {
        given:
        def component = Component.simple("charging", Calculators.fixed("charging", Money.of(12, "PLN")))

        expect:
        component.calculate(Parameters.empty()).money() == Money.of(12, "PLN")
    }
}

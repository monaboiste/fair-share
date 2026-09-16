package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.component.Component
import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class TelcoComponentScenarioSpec extends Specification {
    def "telecom component calculates monthly fee"() {
        given:
        def component = Component.simple("monthly", Calculators.fixed("monthly", Money.of(30, "PLN")))

        expect:
        component.calculate(Parameters.empty()).money() == Money.of(30, "PLN")
    }
}

package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.component.Component
import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class BankingComponentScenarioSpec extends Specification {
    def "loan components compose into a total"() {
        given:
        def component = Component.composite("loan", Component.simple("principal", Calculators.fixed("principal", Money.of(500, "PLN"))), Component.simple("fee", Calculators.fixed("fee", Money.of(20, "PLN"))))

        expect:
        component.calculate(Parameters.empty()).money() == Money.of(520, "PLN")
    }
}

package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.component.Component
import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class LogisticsShipmentScenarioSpec extends Specification {
    def "shipment component composes base and surcharge"() {
        given:
        def component = Component.composite("shipment", Component.simple("base", Calculators.fixed("base", Money.of(25, "PLN"))), Component.simple("surcharge", Calculators.fixed("surcharge", Money.of(5, "PLN"))))

        expect:
        component.calculate(Parameters.empty()).money() == Money.of(30, "PLN")
    }
}

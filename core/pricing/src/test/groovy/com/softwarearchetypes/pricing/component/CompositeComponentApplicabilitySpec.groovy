package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class CompositeComponentApplicabilitySpec extends Specification {
    def "inapplicable composite contributes zero"() {
        given:
        def fee = Component.simple("fee", Calculators.fixed("fee", Money.of(10, "PLN")))
        def component = Component.composite("premium", Map.of(), ApplicabilityConstraint.equalsTo("plan", "premium"), fee)

        expect:
        component.calculate(Parameters.of("plan", plan)).money() == Money.of(expected, "PLN")

        where:
        plan      | expected
        "premium" | 10
        "standard" | 0
    }
}

package com.github.monaboiste.fairshare.pricing.component

import static com.github.monaboiste.fairshare.pricing.calculation.PricingContext.CURRENCY

import com.github.monaboiste.fairshare.pricing.calculation.Calculator
import com.github.monaboiste.fairshare.pricing.calculation.Calculators
import com.github.monaboiste.fairshare.pricing.calculation.Interpretation
import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult
import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ComponentSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")

    def "simple component delegates calculation to its wrapped calculator"() {
        given:
        Calculator calculator = Calculators.fixed("fixed-20", Money.of(BigDecimal.valueOf(20), "PLN"))
        SimpleComponent component = SimpleComponent.of("base-fee", calculator)

        and:
        PricingResult result = component.calculate(Parameters.of(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(20), "PLN")
    }

    def "simple component breakdown has no children"() {
        given:
        Calculator calculator = Calculators.fixed("fixed-50", Money.of(BigDecimal.valueOf(50), "PLN"))
        SimpleComponent component = SimpleComponent.of("service-fee", calculator)

        and:
        ComponentBreakdown breakdown = component.calculateBreakdown(Parameters.of(CURRENCY, PLN))

        expect:
        breakdown.name() == "service-fee"
        breakdown.total() == Money.of(BigDecimal.valueOf(50), "PLN")
        breakdown.children().isEmpty()
    }

    def "composite component sums children results"() {
        given:
        SimpleComponent fee1 = SimpleComponent.of("fee-1",
                Calculators.fixed("calc-1", Money.of(BigDecimal.valueOf(10), "PLN")))
        SimpleComponent fee2 = SimpleComponent.of("fee-2",
                Calculators.fixed("calc-2", Money.of(BigDecimal.valueOf(30), "PLN")))
        CompositeComponent composite = CompositeComponent.of("total-fees", fee1, fee2)

        and:
        PricingResult result = composite.calculate(Parameters.of(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(40), "PLN")
    }

    def "composite component provides a hierarchical breakdown"() {
        given:
        SimpleComponent fee1 = SimpleComponent.of("maintenance",
                Calculators.fixed("calc-1", Money.of(BigDecimal.valueOf(25), "PLN")))
        SimpleComponent fee2 = SimpleComponent.of("commission",
                Calculators.fixed("calc-2", Money.of(BigDecimal.valueOf(20), "PLN")))

        CompositeComponent baseFee = CompositeComponent.of("base-fee", fee1, fee2)

        SimpleComponent extra = SimpleComponent.of("extra-charge",
                Calculators.fixed("calc-3", Money.of(BigDecimal.valueOf(5), "PLN")))

        CompositeComponent total = CompositeComponent.of("total", baseFee, extra)

        and:
        ComponentBreakdown breakdown = total.calculateBreakdown(Parameters.of(CURRENCY, PLN))

        expect:
        breakdown.name() == "total"
        breakdown.total() == Money.of(BigDecimal.valueOf(50), "PLN")
        breakdown.children().size() == 2

        breakdown.children().find { it.name() == "base-fee" }.total() == Money.of(BigDecimal.valueOf(45), "PLN")
        breakdown.children().find { it.name() == "base-fee" }.children().size() == 2

        breakdown.children().find { it.name() == "base-fee" }.children().find { it.name() == "maintenance" }.total() == Money.of(BigDecimal.valueOf(25), "PLN")
        breakdown.children().find { it.name() == "base-fee" }.children().find { it.name() == "maintenance" }.children().isEmpty()

        breakdown.children().find { it.name() == "extra-charge" }.total() == Money.of(BigDecimal.valueOf(5), "PLN")
        breakdown.children().find { it.name() == "extra-charge" }.children().isEmpty()
    }

    def "composite component enriches child parameters based on declared dependencies"() {
        given:
        Calculator baseCalculator = Calculators.fixed("base", Money.of(BigDecimal.valueOf(100), "PLN"))
        SimpleComponent base = SimpleComponent.of("base-price", baseCalculator)
        Calculator percentageCalc = Calculators.percentage("vat", BigDecimal.valueOf(23))
        SimpleComponent vat = SimpleComponent.of("vat", percentageCalc)
        def dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "vat", Map.of(
                "baseAmount", ParameterExpression.valueOf("base-price")
        ))
        CompositeComponent total = CompositeComponent.of("total-with-vat", dependencies, base, vat)

        and:
        PricingResult result = total.calculate(Parameters.of(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(123), "PLN")
    }

    def "composite component supports sum-of dependencies"() {
        given:
        SimpleComponent fee1 = SimpleComponent.of("fee-1",
                Calculators.fixed("c1", Money.of(BigDecimal.valueOf(50), "PLN")))
        SimpleComponent fee2 = SimpleComponent.of("fee-2",
                Calculators.fixed("c2", Money.of(BigDecimal.valueOf(30), "PLN")))
        SimpleComponent tax = SimpleComponent.of("tax",
                Calculators.percentage("tax-calc", BigDecimal.valueOf(10)))
        def dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "tax", Map.of(
                "baseAmount", ParameterExpression.sumOf("fee-1", "fee-2")
        ))
        CompositeComponent total = CompositeComponent.of("total-with-tax", dependencies, fee1, fee2, tax)

        and:
        PricingResult result = total.calculate(Parameters.of(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(88), "PLN")
    }

    def "composite component supports difference-of dependencies"() {
        given:
        SimpleComponent revenue = SimpleComponent.of("revenue",
                Calculators.fixed("rev", Money.of(BigDecimal.valueOf(1000), "PLN")))
        SimpleComponent costs = SimpleComponent.of("costs",
                Calculators.fixed("cost", Money.of(BigDecimal.valueOf(400), "PLN")))
        SimpleComponent profitTax = SimpleComponent.of("profit-tax",
                Calculators.percentage("tax", BigDecimal.valueOf(19)))
        def dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "profit-tax", Map.of(
                "baseAmount", ParameterExpression.differenceOf("revenue", "costs")
        ))
        CompositeComponent financials = CompositeComponent.of("financials", dependencies, revenue, costs, profitTax)

        and:
        PricingResult result = financials.calculate(Parameters.of(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(1514), "PLN")
    }

    def "composite component supports product-of dependencies"() {
        given:
        SimpleComponent baseAmount = SimpleComponent.of("base",
                Calculators.fixed("base", Money.of(BigDecimal.valueOf(100), "PLN")))
        SimpleComponent enhanced = SimpleComponent.of("enhanced",
                Calculators.percentage("calc", BigDecimal.valueOf(10)))
        def dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "enhanced", Map.of(
                "baseAmount", ParameterExpression.productOf("base", BigDecimal.valueOf(1.5))
        ))
        CompositeComponent total = CompositeComponent.of("total", dependencies, baseAmount, enhanced)

        and:
        PricingResult result = total.calculate(Parameters.of(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(115), "PLN")
    }


    def "composite component throws when a dependent component has not been calculated yet"() {
        given:
        SimpleComponent comp1 = SimpleComponent.of("comp-1",
                Calculators.fixed("c1", Money.of(BigDecimal.valueOf(100), "PLN")))
        SimpleComponent comp2 = SimpleComponent.of("comp-2",
                Calculators.percentage("c2", BigDecimal.valueOf(10)))
        List<Component> childrenInWrongOrder = List.of(comp2, comp1)

        def dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "comp-2", Map.of("baseAmount", ParameterExpression.valueOf("comp-1")))
        CompositeComponent composite = CompositeComponent.of(
                "invalid-order", dependencies, childrenInWrongOrder
        )

        when:
        composite.calculate(Parameters.of(CURRENCY, PLN))

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Component 'comp-1' has not been calculated yet. Check execution order."
    }

    def "composite component throws when a referenced component is not found"() {
        given:
        SimpleComponent simple = SimpleComponent.of("simple",
                Calculators.percentage("c", BigDecimal.valueOf(10)))

        def dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "simple", Map.of("baseAmount", ParameterExpression.valueOf("non-existent")))
        CompositeComponent composite = CompositeComponent.of(
                "invalid-ref", dependencies, simple
        )

        when:
        composite.calculate(Parameters.of(CURRENCY, PLN))

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Component 'non-existent' not found"
    }


    def "composite component passes parameters to all children"() {
        given:
        Calculator stepCalc1 = Calculators.stepFunction("step1",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(2))

        Calculator stepCalc2 = Calculators.stepFunction("step2",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(3))

        SimpleComponent comp1 = SimpleComponent.of("comp1", stepCalc1)
        SimpleComponent comp2 = SimpleComponent.of("comp2", stepCalc2)

        CompositeComponent composite = CompositeComponent.of("total", comp1, comp2)

        and:
        Parameters params = Parameters.of("quantity", BigDecimal.valueOf(5)).with(CURRENCY, PLN)
        PricingResult result = composite.calculate(params)

        expect:
        result.money() == Money.of(BigDecimal.valueOf(25), "PLN")
    }

    def "simple component maps parameters before delegating"() {
        given:
        Calculator calculator = Calculators.stepFunction("calc",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(3))
        SimpleComponent component = SimpleComponent.of("mapped",
                calculator,
                Map.of("my_quantity", "quantity"))

        and:
        PricingResult result = component.calculate(Parameters.of("my_quantity", BigDecimal.valueOf(5)).with(CURRENCY, PLN))

        expect:
        result.money() == Money.of(BigDecimal.valueOf(15), "PLN")
    }

    def "simple component passes unmapped parameters through"() {
        given:
        Calculator calculator = Calculators.stepFunction("calc",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(2))
        SimpleComponent component = SimpleComponent.of("partial-map",
                calculator,
                Map.of("my_qty", "quantity"))

        and:
        Parameters params = Parameters.of("my_qty", BigDecimal.valueOf(10)).with(CURRENCY, PLN)
                .with("time", BigDecimal.valueOf(5))

        PricingResult result = component.calculate(params)

        expect:
        result.money() == Money.of(BigDecimal.valueOf(20), "PLN")
    }


    def "simple component converts marginal to total using an adapter"() {
        given:
        Calculator marginalCalc = Calculators.stepFunction("marginal",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(0.50),
                Interpretation.MARGINAL)

        SimpleComponent component = SimpleComponent.of("marginal-comp", marginalCalc)

        and:
        PricingResult resultAsTotal = component.calculate(
                Parameters.of("quantity", BigDecimal.valueOf(10)).with(CURRENCY, PLN))

        expect:
        resultAsTotal.money() == Money.of(BigDecimal.valueOf(27.5), "PLN")
    }

    def "composite component works with children that have parameter mappings"() {
        given:
        Calculator calc1 = Calculators.stepFunction("calc1",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(2))

        Calculator calc2 = Calculators.stepFunction("calc2",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(3))
        SimpleComponent tier1 = SimpleComponent.of("tier1", calc1,
                Map.of("tier1_qty", "quantity"))
        SimpleComponent tier2 = SimpleComponent.of("tier2", calc2,
                Map.of("tier2_qty", "quantity"))
        CompositeComponent composite = CompositeComponent.of("total", tier1, tier2)

        and:
        Parameters params = Parameters.of("tier1_qty", BigDecimal.valueOf(5)).with(CURRENCY, PLN)
                .with("tier2_qty", BigDecimal.valueOf(3))

        PricingResult result = composite.calculate(params)

        expect:
        result.money() == Money.of(BigDecimal.valueOf(19), "PLN")
    }

}

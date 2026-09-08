package com.softwarearchetypes.pricing

import static com.softwarearchetypes.pricing.ComponentBreakdownAssert.assertThat

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.util.List
import java.util.Map
import spock.lang.Specification

class ComponentSpec extends Specification {
    def "simple component delegates calculation to its wrapped calculator"() {
        given:
        Calculator calculator = new SimpleFixedCalculator("fixed-20", Money.of(BigDecimal.valueOf(20), "PLN"))
        SimpleComponent component = SimpleComponent.of("base-fee", calculator)
        and:
        Money result = component.calculate(Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(20), "PLN")
        assert component.interpretation() == Interpretation.TOTAL
    }
    def "simple component breakdown has no children"() {
        given:
        Calculator calculator = new SimpleFixedCalculator("fixed-50", Money.of(BigDecimal.valueOf(50), "PLN"))
        SimpleComponent component = SimpleComponent.of("service-fee", calculator)
        and:
        ComponentBreakdown breakdown = component.calculateBreakdown(Parameters.empty())
        and:
        assertThat(breakdown)
                .hasName("service-fee")
                .hasTotal(Money.of(BigDecimal.valueOf(50), "PLN"))
                .hasNoChildren()
    }
    def "composite component sums children results"() {
        given:
        SimpleComponent fee1 = SimpleComponent.of("fee-1",
                new SimpleFixedCalculator("calc-1", Money.of(BigDecimal.valueOf(10), "PLN")))
        SimpleComponent fee2 = SimpleComponent.of("fee-2",
                new SimpleFixedCalculator("calc-2", Money.of(BigDecimal.valueOf(30), "PLN")))
        CompositeComponent composite = CompositeComponent.of("total-fees", fee1, fee2)
        and:
        Money result = composite.calculate(Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(40), "PLN")
    }
    def "composite component provides a hierarchical breakdown"() {
        given:
        SimpleComponent fee1 = SimpleComponent.of("maintenance",
                new SimpleFixedCalculator("calc-1", Money.of(BigDecimal.valueOf(25), "PLN")))
        SimpleComponent fee2 = SimpleComponent.of("commission",
                new SimpleFixedCalculator("calc-2", Money.of(BigDecimal.valueOf(20), "PLN")))

        CompositeComponent baseFee = CompositeComponent.of("base-fee", fee1, fee2)

        SimpleComponent extra = SimpleComponent.of("extra-charge",
                new SimpleFixedCalculator("calc-3", Money.of(BigDecimal.valueOf(5), "PLN")))

        CompositeComponent total = CompositeComponent.of("total", baseFee, extra)
        and:
        ComponentBreakdown breakdown = total.calculateBreakdown(Parameters.empty())
        and:
        assertThat(breakdown)
                .hasName("total")
                .hasTotal(Money.of(BigDecimal.valueOf(50), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("base-fee")
                .hasTotal(Money.of(BigDecimal.valueOf(45), "PLN"))
                .hasChildrenCount(2)

        assertThat(breakdown)
                .child("base-fee")
                .child("maintenance")
                .hasTotal(Money.of(BigDecimal.valueOf(25), "PLN"))
                .hasNoChildren()

        assertThat(breakdown)
                .child("extra-charge")
                .hasTotal(Money.of(BigDecimal.valueOf(5), "PLN"))
                .hasNoChildren()
    }
    def "composite component enriches child parameters based on declared dependencies"() {
        given:
        Calculator baseCalculator = new SimpleFixedCalculator("base", Money.of(BigDecimal.valueOf(100), "PLN"))
        SimpleComponent base = SimpleComponent.of("base-price", baseCalculator)
        Calculator percentageCalc = new PercentageCalculator("vat", BigDecimal.valueOf(23))
        SimpleComponent vat = SimpleComponent.of("vat", percentageCalc)
        CompositeComponent total = CompositeComponent.of(
                "total-with-vat",
                Map.of(
                        "vat", Map.of(
                                "baseAmount", new ValueOf("base-price")
                        )
                ),
                base, vat
        )
        and:
        Money result = total.calculate(Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(123), "PLN")
    }
    def "composite component supports sum-of dependencies"() {
        given:
        SimpleComponent fee1 = SimpleComponent.of("fee-1",
                new SimpleFixedCalculator("c1", Money.of(BigDecimal.valueOf(50), "PLN")))
        SimpleComponent fee2 = SimpleComponent.of("fee-2",
                new SimpleFixedCalculator("c2", Money.of(BigDecimal.valueOf(30), "PLN")))
        SimpleComponent tax = SimpleComponent.of("tax",
                new PercentageCalculator("tax-calc", BigDecimal.valueOf(10)))
        CompositeComponent total = CompositeComponent.of(
                "total-with-tax",
                Map.of(
                        "tax", Map.of(
                                "baseAmount", new SumOf("fee-1", "fee-2")
                        )
                ),
                fee1, fee2, tax
        )
        and:
        Money result = total.calculate(Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(88), "PLN")
    }
    def "composite component supports difference-of dependencies"() {
        given:
        SimpleComponent revenue = SimpleComponent.of("revenue",
                new SimpleFixedCalculator("rev", Money.of(BigDecimal.valueOf(1000), "PLN")))
        SimpleComponent costs = SimpleComponent.of("costs",
                new SimpleFixedCalculator("cost", Money.of(BigDecimal.valueOf(400), "PLN")))
        SimpleComponent profitTax = SimpleComponent.of("profit-tax",
                new PercentageCalculator("tax", BigDecimal.valueOf(19)))
        CompositeComponent financials = CompositeComponent.of(
                "financials",
                Map.of(
                        "profit-tax", Map.of(
                                "baseAmount", new DifferenceOf("revenue", "costs")
                        )
                ),
                revenue, costs, profitTax
        )
        and:
        Money result = financials.calculate(Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(1514), "PLN")
    }
    def "composite component supports product-of dependencies"() {
        given:
        SimpleComponent baseAmount = SimpleComponent.of("base",
                new SimpleFixedCalculator("base", Money.of(BigDecimal.valueOf(100), "PLN")))
        SimpleComponent enhanced = SimpleComponent.of("enhanced",
                new PercentageCalculator("calc", BigDecimal.valueOf(10)))
        CompositeComponent total = CompositeComponent.of(
                "total",
                Map.of(
                        "enhanced", Map.of(
                                "baseAmount", new ProductOf("base", BigDecimal.valueOf(1.5))
                        )
                ),
                baseAmount, enhanced
        )
        and:
        Money result = total.calculate(Parameters.empty())
        and:
        assert result == Money.of(BigDecimal.valueOf(115), "PLN")
    }
    def "composite component handles mixed interpretations"() {
        given:
        Calculator totalCalc = new SimpleFixedCalculator("total", Money.of(BigDecimal.valueOf(100), "PLN"))
        SimpleComponent totalComponent = SimpleComponent.of("total-comp", totalCalc)

        Calculator unitCalc = new SimpleFixedCalculator("unit",
                Money.of(BigDecimal.valueOf(10), "PLN"),
                Interpretation.UNIT)
        SimpleComponent unitComponent = SimpleComponent.of("unit-comp", unitCalc)
        and:
        CompositeComponent composite = CompositeComponent.of("mixed", totalComponent, unitComponent)
        and:
        assert composite.interpretation() == Interpretation.TOTAL
        Money result = composite.calculate(Parameters.of("quantity", BigDecimal.valueOf(5)))
        assert result == Money.of(BigDecimal.valueOf(150), "PLN")
    }
    def "composite component throws when a dependent component has not been calculated yet"() {
        given:
        SimpleComponent comp1 = SimpleComponent.of("comp-1",
                new SimpleFixedCalculator("c1", Money.of(BigDecimal.valueOf(100), "PLN")))
        SimpleComponent comp2 = SimpleComponent.of("comp-2",
                new PercentageCalculator("c2", BigDecimal.valueOf(10)))
        List<Component> childrenInWrongOrder = List.of(comp2, comp1)

        CompositeComponent composite = CompositeComponent.of(
                "invalid-order",
                Map.of(
                        "comp-2", Map.of(
                                "baseAmount", new ValueOf("comp-1")
                        )
                ),
                childrenInWrongOrder
        )
        when:
        composite.calculate(Parameters.empty())

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Component 'comp-1' has not been calculated yet. Check execution order."
    }
    def "composite component throws when a referenced component is not found"() {
        given:
        SimpleComponent comp = SimpleComponent.of("comp",
                new PercentageCalculator("c", BigDecimal.valueOf(10)))

        CompositeComponent composite = CompositeComponent.of(
                "invalid-ref",
                Map.of(
                        "comp", Map.of(
                                "baseAmount", new ValueOf("non-existent")
                        )
                ),
                comp
        )
        when:
        composite.calculate(Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Component 'non-existent' not found"
    }
    def "composite component always returns total interpretation"() {
        given:
        Calculator calc1 = new SimpleFixedCalculator("c1",
                Money.of(BigDecimal.valueOf(10), "PLN"),
                Interpretation.UNIT)
        Calculator calc2 = new SimpleFixedCalculator("c2",
                Money.of(BigDecimal.valueOf(5), "PLN"),
                Interpretation.UNIT)

        SimpleComponent comp1 = SimpleComponent.of("comp1", calc1)
        SimpleComponent comp2 = SimpleComponent.of("comp2", calc2)
        and:
        CompositeComponent composite = CompositeComponent.of("composite", comp1, comp2)
        and:
        assert composite.interpretation() == Interpretation.TOTAL
        Money result = composite.calculate(Parameters.of("quantity", BigDecimal.valueOf(5)))
        assert result == Money.of(BigDecimal.valueOf(75), "PLN")
    }
    def "composite component passes parameters to all children"() {
        given:
        Calculator stepCalc1 = new StepFunctionCalculator("step1",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(2))

        Calculator stepCalc2 = new StepFunctionCalculator("step2",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(3))

        SimpleComponent comp1 = SimpleComponent.of("comp1", stepCalc1)
        SimpleComponent comp2 = SimpleComponent.of("comp2", stepCalc2)

        CompositeComponent composite = CompositeComponent.of("total", comp1, comp2)
        and:
        Parameters params = Parameters.of("quantity", BigDecimal.valueOf(5))
        Money result = composite.calculate(params)
        and:
        assert result == Money.of(BigDecimal.valueOf(25), "PLN")
    }
    def "composite component throws when it has no children"() {
        given:
        CompositeComponent empty = CompositeComponent.of("empty", List.of())
        when:
        empty.calculate(Parameters.empty())

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Composite component empty has no children"
    }
    def "simple component maps parameters before delegating"() {
        given:
        Calculator calculator = new StepFunctionCalculator("calc",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(3))
        SimpleComponent component = SimpleComponent.of("mapped",
                calculator,
                Map.of("my_quantity", "quantity"))
        and:
        Money result = component.calculate(Parameters.of("my_quantity", BigDecimal.valueOf(5)))
        and:
        assert result == Money.of(BigDecimal.valueOf(15), "PLN")
    }
    def "simple component passes unmapped parameters through"() {
        given:
        Calculator calculator = new StepFunctionCalculator("calc",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(2))
        SimpleComponent component = SimpleComponent.of("partial-map",
                calculator,
                Map.of("my_qty", "quantity"))
        and:
        Parameters params = Parameters.of("my_qty", BigDecimal.valueOf(10))
                .with("time", BigDecimal.valueOf(5))

        Money result = component.calculate(params)
        and:
        assert result == Money.of(BigDecimal.valueOf(20), "PLN")
    }
    def "simple component converts to the requested interpretation using adapters"() {
        given:
        Calculator unitPriceCalc = new SimpleFixedCalculator("unit",
                Money.of(BigDecimal.valueOf(10), "PLN"),
                Interpretation.UNIT)

        SimpleComponent component = SimpleComponent.of("unit-comp", unitPriceCalc)
        and:
        Money resultAsTotal = component.calculate(
                Parameters.of("quantity", BigDecimal.valueOf(5)),
                Interpretation.TOTAL
        )
        and:
        assert resultAsTotal == Money.of(BigDecimal.valueOf(50), "PLN")
        and:
        Money resultAsUnit = component.calculate(
                Parameters.of("quantity", BigDecimal.valueOf(5)),
                Interpretation.UNIT
        )
        and:
        assert resultAsUnit == Money.of(BigDecimal.valueOf(10), "PLN")
    }
    def "simple component converts marginal to total using an adapter"() {
        given:
        Calculator marginalCalc = new StepFunctionCalculator("marginal",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(0.50),
                Interpretation.MARGINAL)

        SimpleComponent component = SimpleComponent.of("marginal-comp", marginalCalc)
        and:
        Money resultAsTotal = component.calculate(
                Parameters.of("quantity", BigDecimal.valueOf(10)),
                Interpretation.TOTAL
        )

        assert resultAsTotal == Money.of(BigDecimal.valueOf(27.5), "PLN")
    }
    def "composite component works with children that have parameter mappings"() {
        given:
        Calculator calc1 = new StepFunctionCalculator("calc1",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(2))

        Calculator calc2 = new StepFunctionCalculator("calc2",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(3))
        SimpleComponent tier1 = SimpleComponent.of("tier1", calc1,
                Map.of("tier1_qty", "quantity"))
        SimpleComponent tier2 = SimpleComponent.of("tier2", calc2,
                Map.of("tier2_qty", "quantity"))
        CompositeComponent composite = CompositeComponent.of("total", tier1, tier2)
        and:
        Parameters params = Parameters.of("tier1_qty", BigDecimal.valueOf(5))
                .with("tier2_qty", BigDecimal.valueOf(3))

        Money result = composite.calculate(params)
        and:
        assert result == Money.of(BigDecimal.valueOf(19), "PLN")
    }
    def "composite component converts children with different interpretations to total"() {
        given:
        Calculator marginalCalc = new StepFunctionCalculator("marginal",
                Money.of(BigDecimal.ZERO, "PLN"),
                BigDecimal.ONE,
                BigDecimal.valueOf(1.0),
                Interpretation.MARGINAL)

        SimpleComponent marginalComp = SimpleComponent.of("marginal", marginalCalc)
        Calculator unitCalc = new SimpleFixedCalculator("unit",
                Money.of(BigDecimal.valueOf(5), "PLN"),
                Interpretation.UNIT)

        SimpleComponent unitComp = SimpleComponent.of("unit", unitCalc)
        Calculator totalCalc = new SimpleFixedCalculator("total",
                Money.of(BigDecimal.valueOf(10), "PLN"))

        SimpleComponent totalComp = SimpleComponent.of("total", totalCalc)
        and:
        CompositeComponent composite = CompositeComponent.of("mixed",
                marginalComp, unitComp, totalComp)
        and:
        Parameters params = Parameters.of("quantity", BigDecimal.valueOf(3))
        Money result = composite.calculate(params)
        assert result == Money.of(BigDecimal.valueOf(31), "PLN")
    }
}

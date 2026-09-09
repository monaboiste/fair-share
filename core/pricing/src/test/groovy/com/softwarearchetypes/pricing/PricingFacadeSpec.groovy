package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import spock.lang.Specification

class PricingFacadeSpec extends Specification {

    private final PricingFacade facade = PricingTestConfiguration.inMemory(Clock.systemUTC())

    def "available calculators includes the pre-registered default calculators"() {
        given:
        facade.addCalculator(
                "simple-fixed-20",
                CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(20), "PLN")))
        facade.addCalculator(
                "simple-interest-6",
                CalculatorType.SIMPLE_INTEREST,
                Parameters.of("annualRate", BigDecimal.valueOf(6)))

        and:
        List<CalculatorView> views = facade.availableCalculators()

        expect:
        views.size() == 2
        views.any { it.name() == "simple-fixed-20" }
        views.any { it.name() == "simple-interest-6" }
    }

    def "available calculators grows when a new calculator is registered"() {
        given:
        int before = facade.availableCalculators().size()
        facade.addCalculator("extra-fixed", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(50, "PLN")))

        and:
        List<CalculatorView> views = facade.availableCalculators()

        expect:
        views.size() == before + 1
        views.any { it.name() == "extra-fixed" }
    }

    def "available calculators includes the correct type for each entry"() {
        given:
        facade.addCalculator("my-fixed", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(100, "PLN")))

        and:
        CalculatorView view = facade.availableCalculators().find { it.name() == "my-fixed" }

        expect:
        view != null
        view.type() == CalculatorType.SIMPLE_FIXED
        view.description() != null
        view.calculatorId() != null
    }

    def "list calculators with descriptions groups by calculator type"() {
        given:
        facade.addCalculator("fixed-extra-1", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(10, "PLN")))
        facade.addCalculator("fixed-extra-2", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(20, "PLN")))
        facade.addCalculator("pct-extra", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(5)))

        and:
        Map<CalculatorType, List<CalculatorView>> grouped = facade.listCalculatorsWithDescriptions()

        expect:
        grouped.get(CalculatorType.SIMPLE_FIXED).size() >= 2
        grouped.get(CalculatorType.PERCENTAGE).size() >= 1
    }

    def "available calculator types returns all defined types"() {
        given:
        List<CalculatorType> types = facade.availableCalculatorTypes()

        expect:
        types.containsAll([
                CalculatorType.SIMPLE_FIXED,
                CalculatorType.STEP_FUNCTION,
                CalculatorType.PERCENTAGE,
                CalculatorType.COMPOSITE,
                CalculatorType.DAILY_INCREMENT,
                CalculatorType.CONTINUOUS_LINEAR_TIME
        ])
    }

    def "calculate throws when calculator name is not found"() {
        when:
        facade.calculate("nonexistent", Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("nonexistent")
    }

    def "calculateTotal throws when calculator name is not found"() {
        when:
        facade.calculateTotal("nonexistent", Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("nonexistent")
    }

    def "calculateUnitPrice throws when calculator name is not found"() {
        when:
        facade.calculateUnitPrice("nonexistent", Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("nonexistent")
    }

    def "calculateMarginal throws when calculator name is not found"() {
        when:
        facade.calculateMarginal("nonexistent", Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("nonexistent")
    }

    def "calculateComponent throws when component name is not found"() {
        when:
        facade.calculateComponent("nonexistent", Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("nonexistent")
    }

    def "calculateComponentBreakdown throws when component name is not found"() {
        when:
        facade.calculateComponentBreakdown("nonexistent", Parameters.empty())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("nonexistent")
    }

    def "addCalculator returns the created calculator"() {
        given:
        Calculator calc = facade.addCalculator("my-calc", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(99, "PLN")))

        expect:
        calc != null
        calc.name() == "my-calc"
        calc.getType() == CalculatorType.SIMPLE_FIXED
    }

    def "calculate delegates to the named calculator"() {
        given:
        facade.addCalculator("flat-100", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(100, "PLN")))

        and:
        Money result = facade.calculate("flat-100", Parameters.empty())

        expect:
        result == Money.of(100, "PLN")
    }
}

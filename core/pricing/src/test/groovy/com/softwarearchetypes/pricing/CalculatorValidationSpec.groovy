package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import spock.lang.Specification

class CalculatorValidationSpec extends Specification {

    def "custom calculator logic is not entered when a declared input is invalid"() {
        given:
        def calculator = new TestCalculator(new ParameterKey<>("quantity", BigDecimal.class))

        when:
        calculator.calculate(value == null ? new Parameters([quantity: null]) : Parameters.of("quantity", value))

        then:
        thrown(IllegalArgumentException)
        calculator.calculations == 0

        where:
        value << [null, "not-a-number"]
    }

    def "parameterless calculator explicitly declares an empty input set"() {
        given:
        def calculator = new TestCalculator()

        expect:
        calculator.inputs() == Set.of()
        calculator.calculate(Parameters.empty()) == Money.of(1, "PLN")
    }

    def "simulation uses the same validation path"() {
        given:
        def calculator = new TestCalculator(new ParameterKey<>("quantity", BigDecimal.class))

        when:
        calculator.simulate([Parameters.of("quantity", "invalid")])

        then:
        thrown(IllegalArgumentException)
        calculator.calculations == 0
    }

    def "compatible duplicate descriptors are accepted"() {
        given:
        def input = new ParameterKey<>("quantity", BigDecimal.class)
        def calculator = new TestCalculator(input, new ParameterKey<>("quantity", BigDecimal.class))

        expect:
        calculator.calculate(Parameters.of("quantity", 2)) == Money.of(1, "PLN")
        calculator.calculations == 1
    }

    def "incompatible duplicate descriptors fail before calculator logic"() {
        given:
        def calculator = new TestCalculator(
                new ParameterKey<>("quantity", BigDecimal.class), new ParameterKey<>("quantity", Money.class))

        when:
        calculator.calculate(Parameters.of("quantity", 2))

        then:
        def error = thrown(IllegalStateException)
        error.message.contains("quantity")
        calculator.calculations == 0
    }

    private static class TestCalculator implements Calculator {
        private final Set<ParameterDefinition> declaredInputs
        int calculations

        TestCalculator(ParameterDefinition... inputs) {
            declaredInputs = inputs as Set
        }

        @Override
        Set<ParameterDefinition> inputs() {
            declaredInputs
        }

        @Override
        Money calculateWithValidInputs(Parameters parameters) {
            calculations++
            Money.of(1, "PLN")
        }

        @Override
        String describe() { "test" }

        @Override
        String formula() { "test" }

        @Override
        Interpretation interpretation() { Interpretation.TOTAL }

        @Override
        CalculatorType getType() { CalculatorType.CUSTOM }

        @Override
        CalculatorId getId() { CalculatorId.generate() }

        @Override
        String name() { "test" }
    }
}

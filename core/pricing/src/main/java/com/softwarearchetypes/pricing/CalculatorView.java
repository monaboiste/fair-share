package com.softwarearchetypes.pricing;

public record CalculatorView(CalculatorId calculatorId, String name, CalculatorType type, String description) {

    static CalculatorView from(Calculator calculator) {
        return new CalculatorView(calculator.getId(), calculator.name(), calculator.getType(), calculator.describe());
    }
}

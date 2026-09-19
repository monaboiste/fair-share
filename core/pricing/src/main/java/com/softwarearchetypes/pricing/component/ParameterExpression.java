package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface ParameterExpression {
    Money evaluate(Map<Component, @Nullable PricingResult> componentResults);

    static ParameterExpression valueOf(String componentName) {
        return new ValueOf(componentName);
    }

    static ParameterExpression sumOf(String... componentNames) {
        return new SumOf(componentNames);
    }

    static ParameterExpression differenceOf(String minuend, String subtrahend) {
        return new DifferenceOf(minuend, subtrahend);
    }

    static ParameterExpression productOf(String componentName, BigDecimal factor) {
        return new ProductOf(componentName, factor);
    }
}

record ValueOf(String componentName) implements ParameterExpression {
    @Override
    public Money evaluate(Map<Component, @Nullable PricingResult> componentResults) {
        Component component = componentResults.keySet().stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));
        PricingResult result = componentResults.get(component);
        if (result == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet. Check execution order.".formatted(componentName));
        }
        return result.money();
    }
}

record SumOf(List<String> componentNames) implements ParameterExpression {
    SumOf(String... componentNames) {
        this(List.of(componentNames));
    }

    SumOf {
        componentNames = List.copyOf(componentNames);
    }

    @Override
    public Money evaluate(Map<Component, @Nullable PricingResult> componentResults) {
        if (componentNames.isEmpty()) {
            throw new IllegalArgumentException("SumOf requires at least one component name");
        }
        Money sum = null;
        for (String name : componentNames) {
            Money value = ParameterExpression.valueOf(name).evaluate(componentResults);
            sum = sum == null ? value : sum.add(value);
        }
        return sum;
    }

    @Override
    public String toString() {
        return "SumOf{componentNames=%s}".formatted(componentNames);
    }
}

record DifferenceOf(String minuendComponent, String subtrahendComponent) implements ParameterExpression {
    @Override
    public Money evaluate(Map<Component, @Nullable PricingResult> componentResults) {
        return findResult(componentResults, minuendComponent)
                .subtract(findResult(componentResults, subtrahendComponent));
    }

    private static Money findResult(Map<Component, @Nullable PricingResult> componentResults, String componentName) {
        Component component = componentResults.keySet().stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));
        PricingResult result = componentResults.get(component);
        if (result == null) {
            throw new IllegalStateException("Component '%s' has not been calculated yet".formatted(componentName));
        }
        return result.money();
    }
}

record ProductOf(String componentName, BigDecimal factor) implements ParameterExpression {
    @Override
    public Money evaluate(Map<Component, @Nullable PricingResult> componentResults) {
        Component component = componentResults.keySet().stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));
        PricingResult result = componentResults.get(component);
        if (result == null) {
            throw new IllegalStateException("Component '%s' has not been calculated yet".formatted(componentName));
        }
        return result.money().multiply(factor);
    }
}

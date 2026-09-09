package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Computes a parameter value from component results. */
public sealed interface ParameterValue permits ValueOf, SumOf, DifferenceOf, ProductOf {

    /** Returns this expression's value. */
    Money evaluate(Map<Component, @Nullable Money> componentResults);
}

record DifferenceOf(String minuendComponent, String subtrahendComponent) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, @Nullable Money> componentResults) {
        Component minuend = componentResults.keySet().stream()
                .filter(c -> c.name().equals(minuendComponent))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Component '%s' not found".formatted(minuendComponent)));

        Component subtrahend = componentResults.keySet().stream()
                .filter(c -> c.name().equals(subtrahendComponent))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Component '%s' not found".formatted(subtrahendComponent)));

        Money minuendValue = componentResults.get(minuend);
        if (minuendValue == null) {
            throw new IllegalStateException("Component '%s' has not been calculated yet".formatted(minuendComponent));
        }

        Money subtrahendValue = componentResults.get(subtrahend);
        if (subtrahendValue == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet".formatted(subtrahendComponent));
        }

        return minuendValue.subtract(subtrahendValue);
    }
}

record ProductOf(String componentName, BigDecimal factor) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, @Nullable Money> componentResults) {
        Component component = componentResults.keySet().stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));

        Money value = componentResults.get(component);
        if (value == null) {
            throw new IllegalStateException("Component '%s' has not been calculated yet".formatted(componentName));
        }

        return value.multiply(factor);
    }
}

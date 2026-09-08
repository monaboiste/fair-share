package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

public record SumOf(List<String> componentNames) implements ParameterValue {

    public SumOf(String... componentNames) {
        this(List.of(componentNames));
    }

    public SumOf {
        componentNames = List.copyOf(componentNames);
    }

    @Override
    public Money evaluate(Map<Component, Money> componentResults) {
        if (componentNames.isEmpty()) {
            throw new IllegalArgumentException("SumOf requires at least one component name");
        }

        Money sum = null;
        for (String name : componentNames) {
            Component component = componentResults.keySet().stream()
                    .filter(c -> c.name().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(name)));

            Money value = componentResults.get(component);
            if (value == null) {
                throw new IllegalStateException(
                        "Component '%s' has not been calculated yet. Check execution order.".formatted(name));
            }

            sum = (sum == null) ? value : sum.add(value);
        }

        return sum;
    }

    @Override
    @NonNull public String toString() {
        return "SumOf{componentNames=%s}".formatted(componentNames);
    }
}

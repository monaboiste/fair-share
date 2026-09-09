package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record ValueOf(String componentName) implements ParameterValue {

    @Override
    public Money evaluate(Map<Component, @Nullable Money> componentResults) {
        Component component = componentResults.keySet().stream()
                .filter(c -> c.name().equals(componentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));

        Money value = componentResults.get(component);
        if (value == null) {
            throw new IllegalStateException(
                    "Component '%s' has not been calculated yet. Check execution order.".formatted(componentName));
        }

        return value;
    }
}

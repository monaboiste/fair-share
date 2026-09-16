package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Calculator;
import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a semantic part of a price calculation. Components can depend on one another. */
public sealed interface Component permits SimpleComponent, CompositeComponent {

    static Component simple(String name, Calculator calculator) {
        return SimpleComponent.of(name, calculator);
    }

    static Component simple(String name, Calculator calculator, Map<String, String> parameterMappings) {
        return SimpleComponent.of(name, calculator, parameterMappings);
    }

    static Component simple(
            String name, Calculator calculator, Map<String, String> parameterMappings, Validity validity) {
        return SimpleComponent.withInitialVersion(
                name,
                calculator,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                Clock.system(ZoneId.systemDefault()));
    }

    static Component simple(String name, Calculator calculator, ApplicabilityConstraint applicabilityConstraint) {
        return SimpleComponent.withInitialVersion(
                name,
                calculator,
                Map.of(),
                applicabilityConstraint,
                Validity.always(),
                Clock.system(ZoneId.systemDefault()));
    }

    static Component simple(
            String name,
            Calculator calculator,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity) {
        return SimpleComponent.withInitialVersion(
                name,
                calculator,
                parameterMappings,
                applicabilityConstraint,
                validity,
                Clock.system(ZoneId.systemDefault()));
    }

    static Component composite(String name, Component... children) {
        return CompositeComponent.of(name, children);
    }

    static Component composite(
            String name, Map<String, Map<String, ParameterExpression>> dependencies, Component... children) {
        return CompositeComponent.of(name, dependencies, children);
    }

    static Component composite(
            String name,
            Map<String, Map<String, ParameterExpression>> dependencies,
            Validity validity,
            Component... children) {
        return composite(name, dependencies, ApplicabilityConstraint.alwaysTrue(), validity, children);
    }

    static Component composite(
            String name,
            Map<String, Map<String, ParameterExpression>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Component... children) {
        return composite(name, dependencies, applicabilityConstraint, Validity.always(), children);
    }

    static Component composite(
            String name,
            Map<String, Map<String, ParameterExpression>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Component... children) {
        return CompositeComponent.withInitialVersion(
                name,
                List.of(children),
                nameDependencies(dependencies, children),
                applicabilityConstraint,
                validity,
                Clock.system(ZoneId.systemDefault()));
    }

    private static Map<ComponentId, Map<String, ParameterExpression>> nameDependencies(
            Map<String, Map<String, ParameterExpression>> dependencies, Component[] children) {
        Map<ComponentId, Map<String, ParameterExpression>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, ParameterExpression>> entry : dependencies.entrySet()) {
            Component child = Arrays.stream(children)
                    .filter(component -> component.name().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Child component '%s' not found".formatted(entry.getKey())));
            result.put(child.id(), entry.getValue());
        }
        return result;
    }

    ComponentId id();

    String name();

    /**
     * Calculates this component's contribution to the total price. Delegates to calculateBreakdown().result().
     *
     * @param parameters input parameters for calculation
     * @return calculated money amount for this component
     */
    PricingResult calculate(Parameters parameters);

    /** Calculate and return a breakdown showing individual component contributions. */
    ComponentBreakdown calculateBreakdown(Parameters parameters);
}

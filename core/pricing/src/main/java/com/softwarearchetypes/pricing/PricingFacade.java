package com.softwarearchetypes.pricing;

import static java.time.LocalDateTime.now;

import com.softwarearchetypes.quantity.money.Money;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PricingFacade {

    private final CalculatorRepository calculatorRepository;
    private final ComponentRepository componentRepository;
    private final Clock clock;

    PricingFacade(CalculatorRepository calculatorRepository, ComponentRepository componentRepository, Clock clock) {
        this.calculatorRepository = calculatorRepository;
        this.componentRepository = componentRepository;
        this.clock = clock;
    }

    public Calculator addCalculator(Calculator calculator) {
        calculatorRepository.save(calculator);
        return calculator;
    }

    public Money calculate(String calculatorName, Parameters parameters) {
        return calculatorRepository
                .findByName(calculatorName)
                .map(c -> c.calculate(parameters).money())
                .orElseThrow(
                        () -> new IllegalArgumentException("could not find calculator %s".formatted(calculatorName)));
    }

    /**
     * Calculates the total price, automatically wrapping the calculator if it does not already return
     * {@link Interpretation#TOTAL}.
     */
    public Money calculateTotal(String calculatorName, Parameters parameters) {
        Calculator calc = calculatorRepository
                .findByName(calculatorName)
                .orElseThrow(
                        () -> new IllegalArgumentException("could not find calculator %s".formatted(calculatorName)));

        Calculator totalCalc = InterpretationAdapters.adapt(calc, Interpretation.TOTAL, parameters);

        return totalCalc.calculate(parameters).money();
    }

    /**
     * Calculates the average unit price, automatically wrapping the calculator if it does not already return
     * {@link Interpretation#UNIT}.
     */
    public Money calculateUnitPrice(String calculatorName, Parameters parameters) {
        Calculator calc = calculatorRepository
                .findByName(calculatorName)
                .orElseThrow(
                        () -> new IllegalArgumentException("could not find calculator %s".formatted(calculatorName)));

        Calculator unitCalc = InterpretationAdapters.adapt(calc, Interpretation.UNIT, parameters);

        return unitCalc.calculate(parameters).money();
    }

    /**
     * Calculates the marginal price, automatically wrapping the calculator if it does not already return
     * {@link Interpretation#MARGINAL}.
     *
     * <ul>
     *   <li>MARGINAL → MARGINAL: identity
     *   <li>UNIT → MARGINAL: valid for a constant unit price
     *   <li>TOTAL → MARGINAL: derivative - marginal(n) = total(n) − total(n−1)
     * </ul>
     */
    public Money calculateMarginal(String calculatorName, Parameters parameters) {
        Calculator calc = calculatorRepository
                .findByName(calculatorName)
                .orElseThrow(
                        () -> new IllegalArgumentException("could not find calculator %s".formatted(calculatorName)));

        Calculator marginalCalc = InterpretationAdapters.adapt(calc, Interpretation.MARGINAL, parameters);

        return marginalCalc.calculate(parameters).money();
    }

    public Component createSimpleComponent(String componentName, String calculatorName) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                Map.of(),
                ApplicabilityConstraint.alwaysTrue(),
                Validity.from(now(clock)));
    }

    public Component createSimpleComponent(
            String componentName, String calculatorName, Map<String, String> parameterMappings) {
        return createSimpleComponent(
                componentName,
                calculatorName,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                Validity.from(now(clock)));
    }

    public Component createSimpleComponent(
            String componentName, String calculatorName, Map<String, String> parameterMappings, Validity validity) {
        return createSimpleComponent(
                componentName, calculatorName, parameterMappings, ApplicabilityConstraint.alwaysTrue(), validity);
    }

    public Component createSimpleComponent(
            String componentName, String calculatorName, ApplicabilityConstraint applicabilityConstraint) {
        return createSimpleComponent(
                componentName, calculatorName, Map.of(), applicabilityConstraint, Validity.from(now(clock)));
    }

    public Component createSimpleComponent(
            String componentName,
            String calculatorName,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint) {
        return createSimpleComponent(
                componentName, calculatorName, parameterMappings, applicabilityConstraint, Validity.from(now(clock)));
    }

    public Component createSimpleComponent(
            String componentName,
            String calculatorName,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity) {
        Calculator calculator = calculatorRepository
                .findByName(calculatorName)
                .orElseThrow(() -> new IllegalArgumentException("Calculator '%s' not found".formatted(calculatorName)));

        return componentRepository
                .findByName(componentName)
                .map(existing -> {
                    if (!(existing instanceof SimpleComponent simpleComponent)) {
                        throw new IllegalArgumentException(
                                "Component '%s' exists but is not a SimpleComponent".formatted(componentName));
                    }
                    SimpleComponentVersion newVersion = new SimpleComponentVersion(
                            calculator, parameterMappings, applicabilityConstraint, validity, now(clock));
                    SimpleComponent updated = simpleComponent.updateWith(newVersion);
                    componentRepository.save(updated);
                    return updated;
                })
                .orElseGet(() -> {
                    SimpleComponent component = SimpleComponent.withInitialVersion(
                            componentName, calculator, parameterMappings, applicabilityConstraint, validity, clock);
                    componentRepository.save(component);
                    return component;
                });
    }

    public Component createCompositeComponent(String compositeName, String... childComponentNames) {
        return createCompositeComponent(compositeName, Map.of(), childComponentNames);
    }

    public Component createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            String... childComponentNames) {
        return createCompositeComponent(
                compositeName,
                dependencies,
                ApplicabilityConstraint.alwaysTrue(),
                Validity.from(now(clock)),
                childComponentNames);
    }

    public Component createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            Validity validity,
            String... childComponentNames) {
        return createCompositeComponent(
                compositeName, dependencies, ApplicabilityConstraint.alwaysTrue(), validity, childComponentNames);
    }

    public Component createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            String... childComponentNames) {
        return createCompositeComponent(
                compositeName, dependencies, applicabilityConstraint, Validity.from(now(clock)), childComponentNames);
    }

    public Component createCompositeComponent(
            String compositeName,
            Map<String, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            String... childComponentNames) {
        List<Component> children = new ArrayList<>();
        for (String childName : childComponentNames) {
            Component child = componentRepository
                    .findByName(childName)
                    .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(childName)));
            children.add(child);
        }

        Map<ComponentId, Map<String, ParameterValue>> idBasedDeps = new HashMap<>();
        for (Map.Entry<String, Map<String, ParameterValue>> entry : dependencies.entrySet()) {
            String childName = entry.getKey();
            Component child = children.stream()
                    .filter(c -> c.name().equals(childName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Child component '%s' not found in children list".formatted(childName)));
            idBasedDeps.put(child.id(), entry.getValue());
        }

        return componentRepository
                .findByName(compositeName)
                .map(existing -> {
                    if (!(existing instanceof CompositeComponent compositeComponent)) {
                        throw new IllegalArgumentException(
                                "Component '%s' exists but is not a CompositeComponent".formatted(compositeName));
                    }
                    CompositeComponentVersion newVersion = new CompositeComponentVersion(
                            children, idBasedDeps, applicabilityConstraint, validity, now(clock));
                    CompositeComponent updated = compositeComponent.updateWith(newVersion);
                    componentRepository.save(updated);
                    return updated;
                })
                .orElseGet(() -> {
                    CompositeComponent component = CompositeComponent.withInitialVersion(
                            compositeName, children, idBasedDeps, applicabilityConstraint, validity, clock);
                    componentRepository.save(component);
                    return component;
                });
    }

    public Money calculateComponent(String componentName, Parameters parameters) {
        Component component = componentRepository
                .findByName(componentName)
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));

        return component.calculate(parameters).money();
    }

    public ComponentBreakdown calculateComponentBreakdown(String componentName, Parameters parameters) {
        Component component = componentRepository
                .findByName(componentName)
                .orElseThrow(() -> new IllegalArgumentException("Component '%s' not found".formatted(componentName)));

        return component.calculateBreakdown(parameters);
    }
}

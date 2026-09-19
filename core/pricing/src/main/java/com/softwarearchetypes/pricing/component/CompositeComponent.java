package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.pricing.calculation.TotalPrice;
import com.softwarearchetypes.quantity.money.Money;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A composite component with a time-versioned child composition.
 *
 * <p>The composition can change over time by adding, removing, or replacing children.
 */
record CompositeComponent(ComponentId id, String name, List<CompositeComponentVersion> versions) implements Component {

    public CompositeComponent {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Component must have at least one version");
        }
    }

    /** Factory: Create CompositeComponent with initial version and applicability constraint. */
    public static CompositeComponent withInitialVersion(
            String name,
            List<Component> children,
            Map<ComponentId, Map<String, ParameterExpression>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        CompositeComponentVersion initialVersion = new CompositeComponentVersion(
                children, dependencies, applicabilityConstraint, validity, LocalDateTime.now(clock));
        return new CompositeComponent(ComponentId.generate(), name, List.of(initialVersion));
    }

    /** Factory: Create a CompositeComponent with an initial version (always applicable). */
    public static CompositeComponent withInitialVersion(
            String name,
            List<Component> children,
            Map<ComponentId, Map<String, ParameterExpression>> dependencies,
            Validity validity,
            Clock clock) {
        return withInitialVersion(name, children, dependencies, ApplicabilityConstraint.alwaysTrue(), validity, clock);
    }

    /** Factory: Create a CompositeComponent with an initial version (no dependencies, always applicable). */
    public static CompositeComponent withInitialVersion(
            String name, List<Component> children, Validity validity, Clock clock) {
        return withInitialVersion(name, children, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, clock);
    }

    /**
     * Creates CompositeComponent valid "always" (no dependencies). For testing and simple use cases where temporal
     * versioning is not needed.
     */
    public static CompositeComponent of(String name, List<Component> children) {
        return withInitialVersion(name, children, Map.of(), Validity.always(), Clock.system(ZoneId.systemDefault()));
    }

    /**
     * Creates CompositeComponent valid "always" (varargs). For testing and simple use cases where temporal versioning
     * is not needed.
     */
    public static CompositeComponent of(String name, Component... children) {
        return withInitialVersion(
                name, List.of(children), Map.of(), Validity.always(), Clock.system(ZoneId.systemDefault()));
    }

    public static CompositeComponent of(
            String name, Map<String, Map<String, ParameterExpression>> nameDependencies, Component... children) {
        return of(name, nameDependencies, Arrays.asList(children));
    }

    /**
     * Creates a CompositeComponent with name-based dependencies (varargs). Dependencies use child component names (not
     * IDs). Valid "always" - for testing and simple use cases.
     */
    public static CompositeComponent of(
            String name, Map<String, Map<String, ParameterExpression>> nameDependencies, List<Component> children) {
        Map<ComponentId, Map<String, ParameterExpression>> idDependencies = new HashMap<>();

        for (Map.Entry<String, Map<String, ParameterExpression>> entry : nameDependencies.entrySet()) {
            String childName = entry.getKey();
            Component child = children.stream()
                    .filter(c -> c.name().equals(childName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Child component '%s' not found in composite '%s'".formatted(childName, name)));
            idDependencies.put(child.id(), entry.getValue());
        }

        CompositeComponentVersion version = new CompositeComponentVersion(
                children, idDependencies, Validity.always(), LocalDateTime.now(Clock.system(ZoneId.systemDefault())));

        return new CompositeComponent(ComponentId.generate(), name, List.of(version));
    }

    /**
     * Adds a new version to this component (immutable operation). Uses default validation strategy: REJECT_IDENTICAL.
     *
     * @param newVersion version to add
     * @return new CompositeComponent with an added version
     * @throws IllegalArgumentException when a version with identical validity already exists
     */
    public CompositeComponent updateWith(CompositeComponentVersion newVersion) {
        return updateWith(newVersion, VersionUpdateStrategy.REJECT_IDENTICAL);
    }

    /**
     * Adds a new version to this component with a custom validation strategy (immutable operation).
     *
     * @param newVersion version to add
     * @param strategy validation strategy for version conflicts
     * @return new CompositeComponent with an added version
     * @throws IllegalArgumentException when validation fails
     */
    public CompositeComponent updateWith(CompositeComponentVersion newVersion, VersionUpdateStrategy strategy) {
        strategy.validate(versions, newVersion.validity());

        List<CompositeComponentVersion> updated = new ArrayList<>(versions);
        updated.add(newVersion);
        return new CompositeComponent(id, name, updated);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {
        return calculateBreakdown(parameters).result();
    }

    @Override
    public ComponentBreakdown calculateBreakdown(Parameters parameters) {
        LocalDateTime time = ComponentVersion.calculationTime(parameters);
        CompositeComponentVersion version = versionAt(time);

        if (!version.isApplicableFor(parameters)) {
            return new ComponentBreakdown(name, new TotalPrice(Money.zero("PLN")), List.of());
        }
        if (version.children().isEmpty()) {
            throw new IllegalStateException("Composite component %s has no children".formatted(name));
        }

        Map<Component, @Nullable PricingResult> componentResults = new HashMap<>();
        version.children().forEach(child -> componentResults.put(child, null));
        List<ComponentBreakdown> childBreakdowns = new ArrayList<>();
        for (Component child : version.children()) {
            Parameters enrichedParams = enrichParameters(child, parameters, componentResults, version.dependencies());
            ComponentBreakdown childBreakdown = child.calculateBreakdown(enrichedParams);
            componentResults.put(child, childBreakdown.result());
            childBreakdowns.add(childBreakdown);
        }
        Money total = childBreakdowns.stream()
                .map(ComponentBreakdown::total)
                .reduce(Money::add)
                .orElseThrow();
        return new ComponentBreakdown(name, new TotalPrice(total), childBreakdowns);
    }

    /**
     * Finds the version valid at a given point in time. When multiple versions are valid, returns the one with the
     * youngest validity. If validity is identical, uses definedAt as tiebreaker (youngest wins).
     */
    private CompositeComponentVersion versionAt(LocalDateTime time) {
        return versions.stream()
                .filter(v -> v.validity().isValidAt(time))
                .max(Comparator.comparing(
                                (CompositeComponentVersion v) -> v.validity().from(),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(CompositeComponentVersion::definedAt))
                .orElseThrow(() -> new IllegalStateException(
                        "No version of component '%s' (%s) valid at %s".formatted(name, id, time)));
    }

    /** Enrich parameters for a child component based on declared dependencies. */
    private Parameters enrichParameters(
            Component child,
            Parameters baseParameters,
            Map<Component, @Nullable PricingResult> componentResults,
            Map<ComponentId, Map<String, ParameterExpression>> dependencies) {
        Map<String, ParameterExpression> childDependencies = dependencies.get(child.id());

        if (childDependencies == null || childDependencies.isEmpty()) {
            return baseParameters;
        }

        Parameters enriched = baseParameters;
        for (Map.Entry<String, ParameterExpression> entry : childDependencies.entrySet()) {
            String targetParamName = entry.getKey();
            ParameterExpression expression = entry.getValue();

            Money value = expression.evaluate(componentResults);
            enriched = enriched.with(targetParamName, value);
        }

        return enriched;
    }
}

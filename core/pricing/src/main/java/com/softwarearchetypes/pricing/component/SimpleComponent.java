package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Calculator;
import com.softwarearchetypes.pricing.calculation.Interpretation;
import com.softwarearchetypes.pricing.calculation.Parameters;
import com.softwarearchetypes.pricing.calculation.PricingResult;
import com.softwarearchetypes.pricing.calculation.TotalPrice;
import com.softwarearchetypes.quantity.money.Money;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A simple component with a time-versioned calculator configuration.
 *
 * <p>The version valid at the calculation timestamp is selected. If versions overlap, the one with the latest
 * {@code validity.from} takes precedence.
 */
record SimpleComponent(ComponentId id, String name, List<SimpleComponentVersion> versions) implements Component {

    public SimpleComponent {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Component must have at least one version");
        }
    }

    /** Factory: Create SimpleComponent with initial version and applicability constraint. */
    public static SimpleComponent withInitialVersion(
            String name,
            Calculator calculator,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        SimpleComponentVersion initialVersion = new SimpleComponentVersion(
                calculator, parameterMappings, applicabilityConstraint, validity, LocalDateTime.now(clock));
        return new SimpleComponent(ComponentId.generate(), name, List.of(initialVersion));
    }

    /** Factory: Create SimpleComponent with the initial version (always applicable). */
    public static SimpleComponent withInitialVersion(
            String name, Calculator calculator, Map<String, String> parameterMappings, Validity validity, Clock clock) {
        return withInitialVersion(
                name, calculator, parameterMappings, ApplicabilityConstraint.alwaysTrue(), validity, clock);
    }

    /** Factory: Create SimpleComponent with the initial version (no parameter mappings, always applicable). */
    public static SimpleComponent withInitialVersion(
            String name, Calculator calculator, Validity validity, Clock clock) {
        return withInitialVersion(name, calculator, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, clock);
    }

    /**
     * Creates SimpleComponent valid "always" (from MIN to MAX). For testing and simple use cases where temporal
     * versioning is not needed.
     */
    public static SimpleComponent of(String name, Calculator calculator) {
        return withInitialVersion(name, calculator, Map.of(), Validity.always(), Clock.system(ZoneId.systemDefault()));
    }

    /**
     * Creates SimpleComponent valid "always" with parameter mappings. For testing and simple use cases where temporal
     * versioning is not needed.
     */
    public static SimpleComponent of(String name, Calculator calculator, Map<String, String> parameterMappings) {
        return withInitialVersion(
                name, calculator, parameterMappings, Validity.always(), Clock.system(ZoneId.systemDefault()));
    }

    /**
     * Adds a new version to this component (immutable operation). Uses default validation strategy: REJECT_IDENTICAL.
     *
     * @param newVersion version to add
     * @return new SimpleComponent with an added version
     * @throws IllegalArgumentException when a version with identical validity already exists
     */
    public SimpleComponent updateWith(SimpleComponentVersion newVersion) {
        return updateWith(newVersion, VersionUpdateStrategy.REJECT_IDENTICAL);
    }

    /**
     * Adds a new version to this component with a custom validation strategy (immutable operation).
     *
     * @param newVersion version to add
     * @param strategy validation strategy for version conflicts
     * @return new SimpleComponent with an added version
     * @throws IllegalArgumentException when validation fails
     */
    public SimpleComponent updateWith(SimpleComponentVersion newVersion, VersionUpdateStrategy strategy) {
        strategy.validate(versions, newVersion.validity());

        List<SimpleComponentVersion> updated = new ArrayList<>(versions);
        updated.add(newVersion);
        return new SimpleComponent(id, name, updated);
    }

    @Override
    public PricingResult calculate(Parameters parameters) {
        LocalDateTime time = ComponentVersion.calculationTime(parameters);
        SimpleComponentVersion version = versionAt(time);

        if (!version.isApplicableFor(parameters)) {
            return new TotalPrice(Money.zero("PLN"));
        }

        Parameters transformedParams = transformParameters(parameters, version.parameterMappings());
        Calculator adaptedCalculator =
                InterpretationAdapters.adapt(version.calculator(), Interpretation.TOTAL, transformedParams);
        return adaptedCalculator.calculate(transformedParams);
    }

    @Override
    public ComponentBreakdown calculateBreakdown(Parameters parameters) {
        return new ComponentBreakdown(name, calculate(parameters));
    }

    /**
     * Finds the version valid at a given point in time. When multiple versions are valid, returns the one with the
     * youngest validity. If validity is identical, uses definedAt as tiebreaker (youngest wins).
     */
    private SimpleComponentVersion versionAt(LocalDateTime time) {
        return versions.stream()
                .filter(v -> v.validity().isValidAt(time))
                .max(Comparator.comparing(
                                (SimpleComponentVersion v) -> v.validity().from(),
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(SimpleComponentVersion::definedAt))
                .orElseThrow(() -> new IllegalStateException(
                        "No version of component '%s' (%s) valid at %s".formatted(name, id, time)));
    }

    /** Transform parameters from component parameter names to calculator parameter names. */
    private Parameters transformParameters(Parameters original, Map<String, String> parameterMappings) {
        if (parameterMappings.isEmpty()) {
            return original;
        }

        Parameters transformed = Parameters.empty();

        for (Map.Entry<String, String> entry : parameterMappings.entrySet()) {
            String componentParam = entry.getKey();
            String calculatorParam = entry.getValue();

            if (original.contains(componentParam)) {
                @Nullable Object value = original.get(componentParam);
                transformed = transformed.with(calculatorParam, value);
            }
        }

        for (String key : original.keys()) {
            if (!transformed.contains(key) && !parameterMappings.containsKey(key)) {
                transformed = transformed.with(key, original.get(key));
            }
        }

        return transformed;
    }
}

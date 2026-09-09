package com.softwarearchetypes.pricing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Defines a composite component's children, dependencies, validity, and applicability.
 *
 * <p>A version is selected only when both its validity period and applicability constraint match the pricing context.
 * Each child component resolves its own version independently.
 */
record CompositeComponentVersion(
        List<Component> children,
        Map<ComponentId, Map<String, ParameterValue>> dependencies,
        ApplicabilityConstraint applicabilityConstraint,
        Validity validity,
        LocalDateTime definedAt)
        implements ComponentVersion {

    public CompositeComponentVersion {
        children = List.copyOf(children);
        dependencies = Map.copyOf(dependencies.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue()))));
        Objects.requireNonNull(applicabilityConstraint, "applicabilityConstraint cannot be null");
        Objects.requireNonNull(definedAt, "definedAt cannot be null");
    }

    /** Backward-compatible constructor - composite always applicable (no business condition). */
    public CompositeComponentVersion(
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            Validity validity,
            LocalDateTime definedAt) {
        this(children, dependencies, ApplicabilityConstraint.alwaysTrue(), validity, definedAt);
    }

    /**
     * Returns true when this version should be used for the given pricing context. Combines the time dimension
     * (validity) with the business dimension (applicability).
     */
    public boolean isApplicableFor(PricingContext context) {
        return validity.isValidAt(context.timestamp()) && applicabilityConstraint.isSatisfiedBy(context);
    }

    /** Create a version with an explicit applicability constraint. */
    public static CompositeComponentVersion of(
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        return new CompositeComponentVersion(
                children, dependencies, applicabilityConstraint, validity, LocalDateTime.now(clock));
    }

    /** Create a version with clock - definedAt will be set now(clock). */
    public static CompositeComponentVersion of(
            List<Component> children,
            Map<ComponentId, Map<String, ParameterValue>> dependencies,
            Validity validity,
            Clock clock) {
        return new CompositeComponentVersion(
                children, dependencies, ApplicabilityConstraint.alwaysTrue(), validity, LocalDateTime.now(clock));
    }

    /** Create a version without dependencies (children are independent). */
    public static CompositeComponentVersion of(List<Component> children, Validity validity, Clock clock) {
        return new CompositeComponentVersion(
                children, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, LocalDateTime.now(clock));
    }

    /** Create a version without dependencies (children are independent). */
    public static CompositeComponentVersion of(Validity validity, Clock clock, Component... children) {
        return new CompositeComponentVersion(
                List.of(children), Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, LocalDateTime.now(clock));
    }
}

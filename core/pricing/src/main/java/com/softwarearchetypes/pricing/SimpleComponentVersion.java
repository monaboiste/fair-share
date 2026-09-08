package com.softwarearchetypes.pricing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/** Defines a calculator configuration with validity and applicability constraints. */
record SimpleComponentVersion(
        Calculator calculator,
        Map<String, String> parameterMappings,
        ApplicabilityConstraint applicabilityConstraint,
        Validity validity,
        LocalDateTime definedAt)
        implements ComponentVersion {

    public SimpleComponentVersion {
        parameterMappings = Map.copyOf(parameterMappings);
        Objects.requireNonNull(definedAt, "definedAt cannot be null");
        Objects.requireNonNull(applicabilityConstraint, "applicabilityConstraint cannot be null");
    }

    /** Creates a version that always applies. */
    public SimpleComponentVersion(
            Calculator calculator, Map<String, String> parameterMappings, Validity validity, LocalDateTime definedAt) {
        this(calculator, parameterMappings, ApplicabilityConstraint.alwaysTrue(), validity, definedAt);
    }

    /** Returns whether validity and applicability match the context. */
    public boolean isApplicableFor(PricingContext context) {
        return validity.isValidAt(context.timestamp()) && applicabilityConstraint.isSatisfiedBy(context);
    }

    /** Create a version with an explicit applicability constraint. */
    public static SimpleComponentVersion of(
            Calculator calculator,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            Clock clock) {
        return new SimpleComponentVersion(
                calculator, parameterMappings, applicabilityConstraint, validity, LocalDateTime.now(clock));
    }

    /** Create a version that always applies (no business condition). */
    public static SimpleComponentVersion of(
            Calculator calculator, Map<String, String> parameterMappings, Validity validity, Clock clock) {
        return new SimpleComponentVersion(
                calculator,
                parameterMappings,
                ApplicabilityConstraint.alwaysTrue(),
                validity,
                LocalDateTime.now(clock));
    }

    /** Create a version without parameter mappings that always applies. */
    public static SimpleComponentVersion of(Calculator calculator, Validity validity, Clock clock) {
        return new SimpleComponentVersion(
                calculator, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, LocalDateTime.now(clock));
    }
}

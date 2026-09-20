package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/** Defines a calculator configuration with validity and applicability constraints. */
public record SimpleComponentVersion(
        ComponentVersionId id,
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

    public SimpleComponentVersion(
            Calculator calculator,
            Map<String, String> parameterMappings,
            ApplicabilityConstraint applicabilityConstraint,
            Validity validity,
            LocalDateTime definedAt) {
        this(
                ComponentVersionId.generate(),
                calculator,
                parameterMappings,
                applicabilityConstraint,
                validity,
                definedAt);
    }

    /** Creates a version that always applies. */
    public SimpleComponentVersion(
            Calculator calculator, Map<String, String> parameterMappings, Validity validity, LocalDateTime definedAt) {
        this(calculator, parameterMappings, ApplicabilityConstraint.alwaysTrue(), validity, definedAt);
    }

    public static SimpleComponentVersion of(
            ComponentVersionId id, Calculator calculator, Validity validity, LocalDateTime definedAt) {
        return new SimpleComponentVersion(
                id, calculator, Map.of(), ApplicabilityConstraint.alwaysTrue(), validity, definedAt);
    }

    /** Returns whether validity and applicability match the context. */
    public boolean isApplicableFor(Parameters parameters) {
        LocalDateTime time = ComponentVersion.calculationTime(parameters);
        return validity.isValidAt(time) && applicabilityConstraint.isSatisfiedBy(parameters);
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

package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingContext;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Describes a component configuration and its validity period. */
sealed interface ComponentVersion permits SimpleComponentVersion, CompositeComponentVersion {

    ComponentVersionId id();

    /** Returns this version's validity period. */
    Validity validity();

    /** Returns when this version was defined. */
    LocalDateTime definedAt();

    /** Returns this version's business condition, evaluated only after validity selects the version. */
    ApplicabilityConstraint applicabilityConstraint();

    static LocalDateTime calculationTime(Parameters parameters) {
        return parameters.find(PricingContext.TIMESTAMP).orElseGet(() -> LocalDateTime.now(ZoneId.systemDefault()));
    }
}

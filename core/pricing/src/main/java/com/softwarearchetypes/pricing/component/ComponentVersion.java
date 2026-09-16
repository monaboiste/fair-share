package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Parameters;
import java.time.LocalDateTime;

/** Describes a component configuration and its validity period. */
sealed interface ComponentVersion permits SimpleComponentVersion, CompositeComponentVersion {

    ComponentVersionId id();

    /** Returns this version's validity period. */
    Validity validity();

    /** Returns when this version was defined. */
    LocalDateTime definedAt();

    static LocalDateTime calculationTime(Parameters parameters) {
        return parameters.timestamp().orElseGet(LocalDateTime::now);
    }
}

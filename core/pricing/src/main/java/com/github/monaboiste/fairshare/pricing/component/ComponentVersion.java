package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Describes a component configuration and its validity period. */
sealed interface ComponentVersion permits SimpleComponentVersion, CompositeComponentVersion {

    ComponentVersionId id();

    /** Returns this version's validity period. */
    Validity validity();

    /** Returns when this version was defined. */
    LocalDateTime definedAt();

    static LocalDateTime calculationTime(Parameters parameters) {
        if (parameters.contains("timestamp")) {
            return parameters.getLocalDateTime("timestamp");
        }
        return LocalDateTime.now(ZoneId.systemDefault());
    }
}

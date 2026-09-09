package com.softwarearchetypes.pricing;

import java.time.LocalDateTime;

/** Describes a component configuration and its validity period. */
sealed interface ComponentVersion permits SimpleComponentVersion, CompositeComponentVersion {

    /** Returns this version's validity period. */
    Validity validity();

    /** Returns when this version was defined. */
    LocalDateTime definedAt();
}

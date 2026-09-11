package com.softwarearchetypes.graphs.scheduling;

import org.jspecify.annotations.Nullable;

/** A named step in a scheduled process. */
public record ProcessStep(@Nullable String name) {

    public ProcessStep {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Process step name cannot be null or blank");
        }
    }
}

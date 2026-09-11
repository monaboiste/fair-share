package com.softwarearchetypes.graphs.scheduling;

import java.util.List;
import org.jspecify.annotations.Nullable;

record Schedule(List<ProcessStep> steps) {
    Schedule {
        steps = List.copyOf(steps);
    }

    @Nullable ProcessStep first() {
        return steps.isEmpty() ? null : steps.getFirst();
    }

    @Nullable ProcessStep last() {
        return steps.isEmpty() ? null : steps.getLast();
    }

    int size() {
        return steps.size();
    }

    boolean isEmpty() {
        return steps.isEmpty();
    }
}

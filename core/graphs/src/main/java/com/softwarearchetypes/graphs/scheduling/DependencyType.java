package com.softwarearchetypes.graphs.scheduling;

import java.util.Map;
import org.jspecify.annotations.Nullable;

record DependencyType(String name, Map<String, Object> features) {
    DependencyType(@Nullable String name, Map<String, Object> features) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Dependency type name cannot be null or blank");
        }
        this.name = name;
        this.features = Map.copyOf(features);
    }

    DependencyType(String name) {
        this(name, Map.of());
    }

    static DependencyType finishToStart(String description) {
        return new DependencyType("FINISH_TO_START", Map.of("description", description));
    }

    static DependencyType requiredResource(String resourceName) {
        return new DependencyType("REQUIRED_RESOURCE", Map.of("resource", resourceName));
    }

    static DependencyType dataFlow(String dataType) {
        return new DependencyType("DATA_FLOW", Map.of("dataType", dataType));
    }

    static DependencyType custom(String name, Map<String, Object> features) {
        return new DependencyType(name, features);
    }
}

package com.softwarearchetypes.product;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Immutable parameters used to evaluate applicability constraints. */
public record ApplicabilityContext(Map<String, String> parameters) {

    public ApplicabilityContext(@Nullable Map<String, String> parameters) {
        this.parameters = Map.copyOf(parameters != null ? parameters : Map.of());
    }

    public static ApplicabilityContext empty() {
        return new ApplicabilityContext(Map.of());
    }

    public static ApplicabilityContext of(@Nullable Map<String, String> parameters) {
        return new ApplicabilityContext(parameters);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(parameters.get(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return parameters.containsKey(key);
    }

    public Map<String, String> asMap() {
        return parameters;
    }

    @Override
    public String toString() {
        return "ApplicabilityContext" + parameters;
    }
}

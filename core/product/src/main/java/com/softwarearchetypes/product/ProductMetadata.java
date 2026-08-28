package com.softwarearchetypes.product;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Immutable key-value metadata for a product. */
public record ProductMetadata(Map<String, String> asMap) {

    public ProductMetadata {
        asMap = asMap != null ? Map.copyOf(asMap) : Map.of();
    }

    public static ProductMetadata empty() {
        return new ProductMetadata(Map.of());
    }

    public static ProductMetadata of(@Nullable Map<String, String> data) {
        return new ProductMetadata(data);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(asMap.get(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return asMap.getOrDefault(key, defaultValue);
    }

    public boolean has(String key) {
        return asMap.containsKey(key);
    }

    public ProductMetadata with(String key, String value) {
        Map<String, String> newData = new HashMap<>(asMap);
        newData.put(key, value);
        return new ProductMetadata(newData);
    }

    @Override
    @NonNull public String toString() {
        return "ProductMetadata" + asMap;
    }
}

package com.softwarearchetypes.scoring.ast;

import java.util.Objects;

public record Metric(String key) {

    public Metric {
        Objects.requireNonNull(key, "metric key required");
        if (key.isBlank()) {
            throw new IllegalArgumentException("blank metric key");
        }
    }

    public static Metric of(String key) {
        return new Metric(key);
    }

    @Override
    public String toString() {
        return key;
    }
}

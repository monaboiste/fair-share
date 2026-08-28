package com.softwarearchetypes.product;

import org.jspecify.annotations.NonNull;

public record ProductDescription(String value) {

    public ProductDescription {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductDescription cannot be null or blank");
        }
    }

    public static ProductDescription of(String value) {
        return new ProductDescription(value);
    }

    @Override
    @NonNull public String toString() {
        return value;
    }
}

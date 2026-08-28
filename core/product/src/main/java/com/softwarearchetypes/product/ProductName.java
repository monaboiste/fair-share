package com.softwarearchetypes.product;

import org.jspecify.annotations.NonNull;

public record ProductName(String value) {

    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductName cannot be null or blank");
        }
    }

    public static ProductName of(String value) {
        return new ProductName(value);
    }

    @Override
    @NonNull public String toString() {
        return value;
    }
}

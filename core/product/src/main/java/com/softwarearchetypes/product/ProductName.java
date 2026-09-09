package com.softwarearchetypes.product;

public record ProductName(String value) {

    public ProductName {
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProductName cannot be null or blank");
        }
    }

    public static ProductName of(String value) {
        return new ProductName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

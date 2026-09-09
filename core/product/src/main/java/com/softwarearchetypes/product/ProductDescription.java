package com.softwarearchetypes.product;

public record ProductDescription(String value) {

    public ProductDescription {
        if (value.isBlank()) {
            throw new IllegalArgumentException("ProductDescription cannot be null or blank");
        }
    }

    public static ProductDescription of(String value) {
        return new ProductDescription(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

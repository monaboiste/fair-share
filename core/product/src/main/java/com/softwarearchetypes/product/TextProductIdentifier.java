package com.softwarearchetypes.product;

import org.jspecify.annotations.NonNull;

record TextProductIdentifier(String value) implements ProductIdentifier {

    TextProductIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TextProductIdentifier value cannot be null or blank");
        }
    }

    @Override
    public String type() {
        return "TEXT";
    }

    @Override
    @NonNull public String toString() {
        return value;
    }
}

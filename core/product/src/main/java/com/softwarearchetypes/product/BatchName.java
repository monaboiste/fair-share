package com.softwarearchetypes.product;

import org.jspecify.annotations.NonNull;

/** A descriptive batch name. */
record BatchName(String value) {

    BatchName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BatchName cannot be null or blank");
        }
    }

    static BatchName of(String value) {
        return new BatchName(value);
    }

    @Override
    @NonNull public String toString() {
        return value;
    }
}

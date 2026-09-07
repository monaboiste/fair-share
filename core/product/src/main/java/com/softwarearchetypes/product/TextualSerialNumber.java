package com.softwarearchetypes.product;

import org.jspecify.annotations.NonNull;

/** A nonblank serial number without format-specific validation. */
record TextualSerialNumber(String value) implements SerialNumber {

    TextualSerialNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SerialNumber cannot be null or blank");
        }
    }

    static TextualSerialNumber of(String value) {
        return new TextualSerialNumber(value);
    }

    @Override
    public String type() {
        return "TEXTUAL";
    }

    @Override
    @NonNull public String toString() {
        return value;
    }
}

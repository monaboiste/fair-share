package com.softwarearchetypes.product;

/** A nonblank serial number without format-specific validation. */
record TextualSerialNumber(String value) implements SerialNumber {

    TextualSerialNumber {
        if (value.isBlank()) {
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
    public String toString() {
        return value;
    }
}

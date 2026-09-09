package com.softwarearchetypes.product;

import java.util.UUID;

/** A UUID-based product identifier. */
record UuidProductIdentifier(UUID value) implements ProductIdentifier {

    static UuidProductIdentifier random() {
        return new UuidProductIdentifier(UUID.randomUUID());
    }

    static UuidProductIdentifier of(String value) {
        return new UuidProductIdentifier(UUID.fromString(value));
    }

    @Override
    public String type() {
        return "UUID";
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

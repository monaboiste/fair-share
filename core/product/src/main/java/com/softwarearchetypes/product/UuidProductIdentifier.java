package com.softwarearchetypes.product;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

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
    @NonNull public String toString() {
        return value.toString();
    }
}

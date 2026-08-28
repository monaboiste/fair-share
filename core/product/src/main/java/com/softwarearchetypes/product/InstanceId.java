package com.softwarearchetypes.product;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/** Identifies a product or package instance independently of its tracking identifiers. */
record InstanceId(UUID value) {

    static InstanceId newOne() {
        return new InstanceId(UUID.randomUUID());
    }

    static InstanceId of(String value) {
        return new InstanceId(UUID.fromString(value));
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}

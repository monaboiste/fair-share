package com.softwarearchetypes.product;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/** Unique identifier for a Batch. */
record BatchId(UUID value) {

    static BatchId newOne() {
        return new BatchId(UUID.randomUUID());
    }

    static BatchId of(String value) {
        return new BatchId(UUID.fromString(value));
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}

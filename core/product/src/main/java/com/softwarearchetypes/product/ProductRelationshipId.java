package com.softwarearchetypes.product;

import java.util.UUID;

public record ProductRelationshipId(UUID value) {

    public static ProductRelationshipId of(String value) {
        return new ProductRelationshipId(UUID.fromString(value));
    }

    public static ProductRelationshipId newOne() {
        return new ProductRelationshipId(UUID.randomUUID());
    }

    String asString() {
        return value.toString();
    }
}

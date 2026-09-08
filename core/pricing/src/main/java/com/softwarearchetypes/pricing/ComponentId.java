package com.softwarearchetypes.pricing;

import java.util.UUID;

public record ComponentId(UUID id) {

    public static ComponentId generate() {
        return new ComponentId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return id.toString();
    }
}

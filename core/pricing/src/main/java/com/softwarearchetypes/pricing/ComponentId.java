package com.softwarearchetypes.pricing;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

public record ComponentId(UUID id) {

    public static ComponentId generate() {
        return new ComponentId(UUID.randomUUID());
    }

    @Override
    @NonNull public String toString() {
        return id.toString();
    }
}

package com.softwarearchetypes.pricing;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

public record CalculatorId(UUID id) {

    public static CalculatorId generate() {
        return new CalculatorId(UUID.randomUUID());
    }

    @Override
    @NonNull public String toString() {
        return id.toString();
    }
}

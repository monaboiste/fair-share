package com.github.monaboiste.fairshare.pricing.calculation;

import java.util.UUID;

public record CalculatorId(UUID id) {

    public static CalculatorId generate() {
        return new CalculatorId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return id.toString();
    }
}

package com.github.monaboiste.fairshare.settlement;

import java.util.Objects;

public record SettlementRenamed(String name) implements SettlementEvent {
    public SettlementRenamed {
        if (Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

package com.github.monaboiste.fairshare.settlement.domain.event;

import java.time.Instant;

public record SettlementRenamed(String name, Instant occurredAt) implements SettlementEvent {
    public SettlementRenamed {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }

    @Override
    public String type() {
        return "SettlementRenamed";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

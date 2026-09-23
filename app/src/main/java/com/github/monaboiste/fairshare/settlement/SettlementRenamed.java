package com.github.monaboiste.fairshare.settlement;

public record SettlementRenamed(String name) implements SettlementEvent {
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

package com.github.monaboiste.fairshare.settlement.domain.event;

public record SettlementRenamed(String name) implements SettlementEvent {
    @Override
    public String type() {
        return "SettlementRenamed";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

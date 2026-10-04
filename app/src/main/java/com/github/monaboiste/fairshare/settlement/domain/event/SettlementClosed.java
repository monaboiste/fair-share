package com.github.monaboiste.fairshare.settlement.domain.event;

public record SettlementClosed() implements SettlementEvent {
    @Override
    public String type() {
        return "SettlementClosed";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

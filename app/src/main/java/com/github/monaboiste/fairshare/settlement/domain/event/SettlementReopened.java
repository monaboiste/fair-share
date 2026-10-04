package com.github.monaboiste.fairshare.settlement.domain.event;

public record SettlementReopened() implements SettlementEvent {
    @Override
    public String type() {
        return "SettlementReopened";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

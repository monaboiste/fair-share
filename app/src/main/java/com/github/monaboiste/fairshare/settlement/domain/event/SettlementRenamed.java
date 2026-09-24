package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventId;
import java.time.Instant;

public record SettlementRenamed(EventId eventId, String name, Instant occurredAt) implements SettlementEvent {
    public SettlementRenamed(String name, Instant occurredAt) {
        this(EventId.random(), name, occurredAt);
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

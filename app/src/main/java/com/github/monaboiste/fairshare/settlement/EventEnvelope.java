package com.github.monaboiste.fairshare.settlement;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        SettlementId settlementId,
        long sequence,
        Instant occurredAt,
        String type,
        int schemaVersion,
        SettlementEvent payload) {
    public EventEnvelope {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(settlementId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(type);
        Objects.requireNonNull(payload);
        if (sequence < 1 || schemaVersion < 1) {
            throw new IllegalArgumentException("Sequence and schema version must be positive");
        }
    }
}

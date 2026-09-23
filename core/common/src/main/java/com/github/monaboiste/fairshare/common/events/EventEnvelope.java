package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<S, E extends Event>(
        UUID eventId, S streamId, long sequence, Instant occurredAt, E payload) {
    public EventEnvelope {
        if (sequence < 1 || payload.schemaVersion() < 1) {
            throw new IllegalArgumentException("Sequence and schema version must be positive");
        }
    }

    public String type() {
        return payload.type();
    }

    public int schemaVersion() {
        return payload.schemaVersion();
    }
}

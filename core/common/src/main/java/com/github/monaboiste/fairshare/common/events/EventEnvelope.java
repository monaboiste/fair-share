package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;

public record EventEnvelope<S, E extends Event>(EventId eventId, S streamId, long sequence, E payload) {
    public EventEnvelope {
        if (sequence < 1 || payload.schemaVersion() < 1) {
            throw new IllegalArgumentException("Sequence and schema version must be positive");
        }
    }

    public Instant occurredAt() {
        return payload.occurredAt();
    }

    public String type() {
        return payload.type();
    }

    public int schemaVersion() {
        return payload.schemaVersion();
    }
}

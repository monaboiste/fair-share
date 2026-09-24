package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;

public record EventEnvelope<S, E extends Event>(
        S streamId, long sequence, long position, String type, int schemaVersion, E payload) {
    public EventEnvelope {
        if (sequence < 1 || position < 1 || schemaVersion < 1 || type.isBlank()) {
            throw new IllegalArgumentException("Invalid event envelope metadata");
        }
    }

    public EventId eventId() {
        return payload.eventId();
    }

    public Instant occurredAt() {
        return payload.occurredAt();
    }
}

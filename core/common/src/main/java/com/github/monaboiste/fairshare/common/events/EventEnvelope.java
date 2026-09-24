package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;

public record EventEnvelope<S, E extends Event>(S streamId, long sequence, long position, E payload) {
    public EventEnvelope {
        if (sequence < 1
                || position < 1
                || payload.schemaVersion() < 1
                || payload.type().isBlank()) {
            throw new IllegalArgumentException("Invalid event envelope metadata");
        }
    }

    public EventId eventId() {
        return payload.eventId();
    }

    public String type() {
        return payload.type();
    }

    public int schemaVersion() {
        return payload.schemaVersion();
    }

    public Instant occurredAt() {
        return payload.occurredAt();
    }
}

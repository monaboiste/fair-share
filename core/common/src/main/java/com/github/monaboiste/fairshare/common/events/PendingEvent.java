package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;

public record PendingEvent<E extends Event>(EventId eventId, E payload, Instant occurredAt) {}

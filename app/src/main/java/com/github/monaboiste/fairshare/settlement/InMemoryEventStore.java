package com.github.monaboiste.fairshare.settlement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InMemoryEventStore implements EventStore {
    private final Map<SettlementId, List<EventEnvelope>> streams = new HashMap<>();

    @Override
    public synchronized List<EventEnvelope> load(SettlementId id) {
        List<EventEnvelope> events = streams.get(Objects.requireNonNull(id));
        if (events == null) {
            throw new MissingStreamException(id);
        }
        return events;
    }

    @Override
    public synchronized AppendResult append(SettlementId id, long expectedVersion, List<EventEnvelope> events) {
        Objects.requireNonNull(id);
        List<EventEnvelope> previous = streams.get(id);
        long actual = previous == null ? 0 : previous.size();
        if (actual != expectedVersion) {
            throw new VersionConflictException(id, expectedVersion, actual);
        }
        List<EventEnvelope> additions = List.copyOf(events);
        if (additions.isEmpty()) {
            throw new IllegalArgumentException("Append requires events");
        }
        for (int index = 0; index < additions.size(); index++) {
            EventEnvelope envelope = additions.get(index);
            if (!envelope.settlementId().equals(id) || envelope.sequence() != actual + index + 1) {
                throw new IllegalArgumentException("Event stream identifier or sequence mismatch");
            }
        }
        List<EventEnvelope> updated = new java.util.ArrayList<>();
        if (previous != null) {
            updated.addAll(previous);
        }
        updated.addAll(additions);
        streams.put(id, List.copyOf(updated));
        return new AppendResult(additions, updated.size());
    }
}

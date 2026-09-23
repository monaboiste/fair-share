package com.github.monaboiste.fairshare.common.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryEventStore<S, E extends Event> implements EventStore<S, E> {
    private final Map<S, List<EventEnvelope<S, E>>> streams = new HashMap<>();

    @Override
    public synchronized List<EventEnvelope<S, E>> load(S id) {
        List<EventEnvelope<S, E>> events = streams.get(id);
        if (events == null) {
            throw new MissingStreamException(id);
        }
        return events;
    }

    @Override
    public synchronized Map<S, List<EventEnvelope<S, E>>> streams() {
        return Map.copyOf(streams);
    }

    @Override
    public synchronized AppendResult<S, E> append(S id, long expectedVersion, List<EventEnvelope<S, E>> events) {
        List<EventEnvelope<S, E>> previous = streams.get(id);
        long actual = previous == null ? 0 : previous.size();
        if (actual != expectedVersion) {
            throw new VersionConflictException(id, expectedVersion, actual);
        }
        List<EventEnvelope<S, E>> additions = List.copyOf(events);
        if (additions.isEmpty()) {
            throw new IllegalArgumentException("Append requires events");
        }
        for (int index = 0; index < additions.size(); index++) {
            EventEnvelope<S, E> envelope = additions.get(index);
            if (!envelope.streamId().equals(id) || envelope.sequence() != actual + index + 1) {
                throw new IllegalArgumentException("Event stream identifier or sequence mismatch");
            }
        }
        List<EventEnvelope<S, E>> updated = new ArrayList<>();
        if (previous != null) {
            updated.addAll(previous);
        }
        updated.addAll(additions);
        streams.put(id, List.copyOf(updated));
        return new AppendResult<>(additions, updated.size());
    }
}

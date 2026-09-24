package com.github.monaboiste.fairshare.common.events.inmemory;

import com.github.monaboiste.fairshare.common.events.AllEventsReader;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.Event;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.EventType;
import com.github.monaboiste.fairshare.common.events.EventTypes;
import com.github.monaboiste.fairshare.common.events.NewEvent;
import com.github.monaboiste.fairshare.common.events.StreamNotFoundException;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryEventStore<S, E extends Event> implements EventStore<S, E>, AllEventsReader<S, E> {
    private final Map<S, List<EventEnvelope<S, E>>> streams = new HashMap<>();
    private final List<EventEnvelope<S, E>> allEvents = new ArrayList<>();

    @Override
    public synchronized List<EventEnvelope<S, E>> load(S id) {
        List<EventEnvelope<S, E>> history = streams.get(id);
        if (history == null) {
            throw new StreamNotFoundException(id);
        }
        return history;
    }

    @Override
    public synchronized boolean exists(S id) {
        return streams.containsKey(id);
    }

    @Override
    public synchronized List<EventEnvelope<S, E>> readAll(long afterPosition) {
        return allEvents.stream()
                .filter(event -> event.position() > afterPosition)
                .toList();
    }

    @Override
    public synchronized CommitResult<S, E> append(S id, long expectedVersion, List<NewEvent<E>> events) {
        List<EventEnvelope<S, E>> previous = streams.getOrDefault(id, List.of());
        long actual = previous.size();
        if (actual != expectedVersion) {
            throw new VersionConflictException(id, expectedVersion, actual);
        }
        List<NewEvent<E>> additions = List.copyOf(events);
        if (additions.isEmpty()) {
            throw new IllegalArgumentException("Append requires events");
        }
        List<EventEnvelope<S, E>> committed = new ArrayList<>();
        for (NewEvent<E> next : additions) {
            EventType type = EventTypes.of(next.payload());
            committed.add(new EventEnvelope<>(
                    next.eventId(),
                    id,
                    actual + committed.size() + 1,
                    allEvents.size() + committed.size() + 1,
                    type.name(),
                    type.version(),
                    next.payload()));
        }
        List<EventEnvelope<S, E>> updated = new ArrayList<>(previous);
        updated.addAll(committed);
        streams.put(id, List.copyOf(updated));
        allEvents.addAll(committed);
        return new CommitResult<>(committed, updated.size());
    }
}

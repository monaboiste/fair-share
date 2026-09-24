package com.github.monaboiste.fairshare.common.events.inmemory;

import com.github.monaboiste.fairshare.common.events.AllEventsReader;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.CommittedEventsListener;
import com.github.monaboiste.fairshare.common.events.Event;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.EventSubscriptions;
import com.github.monaboiste.fairshare.common.events.PendingEvent;
import com.github.monaboiste.fairshare.common.events.PostCommitPublicationException;
import com.github.monaboiste.fairshare.common.events.StreamNotFoundException;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InMemoryEventStore<S, E extends Event>
        implements EventStore<S, E>, AllEventsReader<S, E>, EventSubscriptions<S, E> {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);

    private final Map<S, List<EventEnvelope<S, E>>> streams = new HashMap<>();
    private final List<EventEnvelope<S, E>> allEvents = new ArrayList<>();
    private final List<CommittedEventsListener<S, E>> listeners = new ArrayList<>();
    private boolean delivering;

    @Override
    public synchronized void subscribe(CommittedEventsListener<S, E> listener) {
        listeners.add(listener);
    }

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
    public synchronized CommitResult<S, E> append(S id, long expectedVersion, List<PendingEvent<E>> events) {
        if (delivering) {
            throw new IllegalStateException("Subscribers must not append during delivery");
        }

        List<EventEnvelope<S, E>> previous = streams.getOrDefault(id, List.of());
        long actualVersion = previous.size();

        if (actualVersion != expectedVersion) {
            throw new VersionConflictException(id, expectedVersion, actualVersion);
        }

        List<PendingEvent<E>> additions = List.copyOf(events);
        if (additions.isEmpty()) {
            throw new IllegalArgumentException("Append requires events");
        }

        List<EventEnvelope<S, E>> committed = commit(id, actualVersion, additions);

        List<EventEnvelope<S, E>> updated = new ArrayList<>(previous.size() + committed.size());
        updated.addAll(previous);
        updated.addAll(committed);

        streams.put(id, List.copyOf(updated));
        allEvents.addAll(committed);

        CommitResult<S, E> result = new CommitResult<>(id, committed, updated.size());
        deliver(result);

        return result;
    }

    private List<EventEnvelope<S, E>> commit(S id, long currentVersion, List<PendingEvent<E>> events) {
        List<EventEnvelope<S, E>> committed = new ArrayList<>(events.size());

        long sequence = currentVersion + 1L;
        long position = allEvents.size() + 1L;

        for (PendingEvent<E> event : events) {
            committed.add(new EventEnvelope<>(
                    id, sequence++, position++, event.eventId(), event.occurredAt(), event.payload()));
        }

        return List.copyOf(committed);
    }

    private void deliver(CommitResult<S, E> result) {
        delivering = true;
        try {
            deliver(result.streamId(), result);
        } finally {
            delivering = false;
        }
    }

    private void deliver(S id, CommitResult<S, E> result) {
        for (CommittedEventsListener<S, E> listener : listeners) {
            try {
                listener.accept(result.events());
            } catch (RuntimeException failure) {
                log.error("Fatal post-commit publication failure at version {}", result.version(), failure);
                throw new PostCommitPublicationException(id, result.version(), failure);
            }
        }
    }
}

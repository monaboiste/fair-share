package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public final class PublishingEventStore<S, E extends Event> implements EventStore<S, E>, AllEventsReader<S, E> {
    private final EventStore<S, E> store;
    private final AllEventsReader<S, E> allEvents;
    private final CommittedEventsListener<S, E> listener;

    public PublishingEventStore(
            EventStore<S, E> store, AllEventsReader<S, E> allEvents, CommittedEventsListener<S, E> listener) {
        this.store = store;
        this.allEvents = allEvents;
        this.listener = listener;
    }

    public static <S, E extends Event, T extends EventStore<S, E> & AllEventsReader<S, E>>
            PublishingEventStore<S, E> of(T store, CommittedEventsListener<S, E> listener) {
        return new PublishingEventStore<>(store, store, listener);
    }

    @Override
    public List<EventEnvelope<S, E>> load(S id) {
        return store.load(id);
    }

    @Override
    public boolean exists(S id) {
        return store.exists(id);
    }

    @Override
    public List<EventEnvelope<S, E>> readAll(long afterPosition) {
        return allEvents.readAll(afterPosition);
    }

    @Override
    public synchronized CommitResult<S, E> append(S id, long expectedVersion, List<NewEvent<E>> events) {
        CommitResult<S, E> committed = store.append(id, expectedVersion, events);
        try {
            listener.accept(committed.events());
        } catch (RuntimeException failure) {
            System.getLogger(getClass().getName())
                    .log(
                            System.Logger.Level.ERROR,
                            "Fatal post-commit publication failure at version " + committed.version(),
                            failure);
            throw new PostCommitPublicationException(id, committed.version(), failure);
        }
        return committed;
    }
}

package com.github.monaboiste.fairshare.common.eventsourcing;

import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.Event;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.PendingEvent;
import com.github.monaboiste.fairshare.common.events.PostCommitPublicationException;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("squid:S119")
public class EventSourcedRepository<ID, E extends Event, A extends AggregateRoot<ID, E>> {
    private final EventStore<ID, E> store;
    private final AggregateFactory<ID, A> factory;

    public EventSourcedRepository(EventStore<ID, E> store, AggregateFactory<ID, A> factory) {
        this.store = store;
        this.factory = factory;
    }

    public Optional<A> findById(ID id) {
        if (!store.exists(id)) {
            return Optional.empty();
        }
        A aggregate = factory.create(id);
        aggregate.replay(store.load(id).stream().map(EventEnvelope::payload).toList());
        return Optional.of(aggregate);
    }

    public CommitResult<ID, E> save(A aggregate) {
        List<PendingEvent<E>> pending = aggregate.pendingEvents();
        if (pending.isEmpty()) {
            return new CommitResult<>(aggregate.id(), List.of(), aggregate.committedVersion());
        }
        try {
            CommitResult<ID, E> result = store.append(aggregate.id(), aggregate.committedVersion(), pending);
            aggregate.markCommitted(result.version());
            return result;
        } catch (PostCommitPublicationException failure) {
            aggregate.markCommitted(failure.committedVersion());
            throw failure;
        }
    }
}

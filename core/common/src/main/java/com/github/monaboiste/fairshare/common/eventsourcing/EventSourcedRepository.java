package com.github.monaboiste.fairshare.common.eventsourcing;

import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.Event;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.NewEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("squid:S119")
public class EventSourcedRepository<ID, E extends Event, A extends AggregateRoot<ID, E>> {
    private final EventStore<ID, E> store;
    private final Supplier<EventId> eventIds;
    private final AggregateFactory<ID, A> factory;

    public EventSourcedRepository(
            EventStore<ID, E> store, Supplier<EventId> eventIds, AggregateFactory<ID, A> factory) {
        this.store = store;
        this.eventIds = eventIds;
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
        List<E> pending = aggregate.pendingEvents();
        if (pending.isEmpty()) {
            return new CommitResult<>(List.of(), aggregate.committedVersion());
        }
        List<NewEvent<E>> events = pending.stream()
                .map(event -> new NewEvent<>(eventIds.get(), event))
                .toList();
        CommitResult<ID, E> result = store.append(aggregate.id(), aggregate.committedVersion(), events);
        aggregate.markCommitted(result.version());
        return result;
    }
}

package com.github.monaboiste.fairshare.common.eventsourcing;

import com.github.monaboiste.fairshare.common.events.Event;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S119")
public abstract class AggregateRoot<ID, E extends Event> {
    private final List<E> pendingEvents = new ArrayList<>();
    private long version;

    public abstract ID id();

    protected abstract void apply(E event);

    protected void register(E event) {
        apply(event);
        pendingEvents.add(event);
        version++;
    }

    void replay(List<E> history) {
        if (version != 0 || !pendingEvents.isEmpty()) {
            throw new IllegalStateException("Replay requires a fresh aggregate");
        }
        history.forEach(this::apply);
        version += history.size();
    }

    void markCommitted(long committedVersion) {
        if (committedVersion != committedVersion() + pendingEvents.size()) {
            throw new IllegalArgumentException("Committed version does not match pending events");
        }
        pendingEvents.clear();
    }

    public long version() {
        return version;
    }

    public long committedVersion() {
        return version - pendingEvents.size();
    }

    public List<E> pendingEvents() {
        return List.copyOf(pendingEvents);
    }
}

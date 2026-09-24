package com.github.monaboiste.fairshare.common.domain;

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

    protected void replay(List<E> history) {
        history.forEach(this::apply);
        version += history.size();
    }

    public long version() {
        return version;
    }

    /** @return version of the stored stream, excluding pending events; the expected version for the next append */
    public long committedVersion() {
        return version - pendingEvents.size();
    }

    public List<E> pendingEvents() {
        return List.copyOf(pendingEvents);
    }

    public List<E> flushPendingEvents() {
        List<E> flushed = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return flushed;
    }
}

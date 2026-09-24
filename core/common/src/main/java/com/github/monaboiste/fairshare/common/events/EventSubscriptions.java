package com.github.monaboiste.fairshare.common.events;

public interface EventSubscriptions<S, E extends Event> {
    void subscribe(CommittedEventsListener<S, E> listener);
}

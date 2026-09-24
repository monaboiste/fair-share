package com.github.monaboiste.fairshare.common.events;

import java.util.List;

@FunctionalInterface
public interface CommittedEventsListener<S, E extends Event> {
    void accept(List<EventEnvelope<S, E>> events);
}

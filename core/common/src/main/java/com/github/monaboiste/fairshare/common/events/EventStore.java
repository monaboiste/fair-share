package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public interface EventStore<S, E extends Event> {
    List<EventEnvelope<S, E>> load(S id);

    AppendResult<S, E> append(S id, long expectedVersion, List<EventEnvelope<S, E>> events);
}

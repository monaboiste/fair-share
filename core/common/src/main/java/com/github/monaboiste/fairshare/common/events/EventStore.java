package com.github.monaboiste.fairshare.common.events;

import java.util.List;
import java.util.Map;

public interface EventStore<S, E extends Event> {
    List<EventEnvelope<S, E>> load(S id);

    default Map<S, List<EventEnvelope<S, E>>> streams() {
        throw new UnsupportedOperationException("Stream enumeration not supported");
    }

    AppendResult<S, E> append(S id, long expectedVersion, List<EventEnvelope<S, E>> events);
}

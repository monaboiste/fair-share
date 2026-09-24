package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public interface EventStore<S, E extends Event> extends EventStreamReader<S, E> {
    CommitResult<S, E> append(S id, long expectedVersion, List<PendingEvent<E>> events);
}

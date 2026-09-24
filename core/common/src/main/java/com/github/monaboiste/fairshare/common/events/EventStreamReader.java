package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public interface EventStreamReader<S, E extends Event> {
    List<EventEnvelope<S, E>> load(S id);

    boolean exists(S id);
}

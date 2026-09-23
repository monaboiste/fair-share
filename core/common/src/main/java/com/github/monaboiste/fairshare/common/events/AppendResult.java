package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public record AppendResult<S, E extends Event>(List<EventEnvelope<S, E>> events, long version) {
    public AppendResult {
        events = List.copyOf(events);
    }
}

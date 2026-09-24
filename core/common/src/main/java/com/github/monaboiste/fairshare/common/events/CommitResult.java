package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public record CommitResult<S, E extends Event>(S streamId, List<EventEnvelope<S, E>> events, long version) {
    public CommitResult {
        events = List.copyOf(events);
    }
}

package com.github.monaboiste.fairshare.settlement;

import java.util.List;

public record AppendResult(List<EventEnvelope> events, long version) {
    public AppendResult {
        events = List.copyOf(events);
    }
}

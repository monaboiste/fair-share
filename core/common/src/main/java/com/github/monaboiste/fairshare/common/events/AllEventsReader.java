package com.github.monaboiste.fairshare.common.events;

import java.util.List;

public interface AllEventsReader<S, E extends Event> {
    List<EventEnvelope<S, E>> readAll(long afterPosition);
}

package com.github.monaboiste.fairshare.common.events;

import java.time.Instant;

public interface Event {
    Instant occurredAt();
}

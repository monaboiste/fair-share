package com.github.monaboiste.fairshare.common.events;

import java.util.UUID;

public record EventId(UUID value) {

    public static EventId random() {
        return new EventId(UUID.randomUUID());
    }
}

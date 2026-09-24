package com.github.monaboiste.fairshare.common.events;

public final class EventTypes {
    private EventTypes() {}

    public static EventType of(Event event) {
        EventType type = event.getClass().getAnnotation(EventType.class);
        if (type == null || type.name().isBlank() || type.version() < 1) {
            throw new IllegalArgumentException("Invalid or missing event type: " + event.getClass());
        }
        return type;
    }
}

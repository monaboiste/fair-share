package com.github.monaboiste.fairshare.common.events;

public record NewEvent<E extends Event>(EventId eventId, E payload) {}

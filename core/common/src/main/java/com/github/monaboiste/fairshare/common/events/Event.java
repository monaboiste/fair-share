package com.github.monaboiste.fairshare.common.events;

public interface Event {
    String type();

    int schemaVersion();
}

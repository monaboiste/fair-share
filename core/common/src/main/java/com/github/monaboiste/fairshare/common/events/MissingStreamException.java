package com.github.monaboiste.fairshare.common.events;

public final class MissingStreamException extends RuntimeException {
    public MissingStreamException(Object id) {
        super("Missing event stream: " + id);
    }
}

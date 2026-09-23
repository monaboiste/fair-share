package com.github.monaboiste.fairshare.common.events;

public final class VersionConflictException extends RuntimeException {
    public VersionConflictException(Object id, long expected, long actual) {
        super("Stream " + id + " expected version " + expected + " but was " + actual);
    }
}

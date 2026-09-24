package com.github.monaboiste.fairshare.common.events;

public final class VersionConflictException extends RuntimeException {
    private final Object streamId;
    private final long expectedVersion;
    private final long actualVersion;

    public VersionConflictException(Object streamId, long expectedVersion, long actualVersion) {
        super("Stream " + streamId + " expected version " + expectedVersion + " but was " + actualVersion);
        this.streamId = streamId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public Object streamId() {
        return streamId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }

    public long actualVersion() {
        return actualVersion;
    }
}

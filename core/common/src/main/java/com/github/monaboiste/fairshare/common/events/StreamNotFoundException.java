package com.github.monaboiste.fairshare.common.events;

public final class StreamNotFoundException extends RuntimeException {
    private final Object streamId;

    public StreamNotFoundException(Object streamId) {
        super("Stream not found: " + streamId);
        this.streamId = streamId;
    }

    public Object streamId() {
        return streamId;
    }
}

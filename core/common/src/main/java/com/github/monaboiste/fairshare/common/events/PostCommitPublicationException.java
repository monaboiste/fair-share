package com.github.monaboiste.fairshare.common.events;

public final class PostCommitPublicationException extends RuntimeException {
    private final Object streamId;
    private final long committedVersion;

    public PostCommitPublicationException(Object streamId, long committedVersion, Throwable cause) {
        super("Stream " + streamId + " committed at version " + committedVersion + " but publication failed", cause);
        this.streamId = streamId;
        this.committedVersion = committedVersion;
    }

    public Object streamId() {
        return streamId;
    }

    public long committedVersion() {
        return committedVersion;
    }
}

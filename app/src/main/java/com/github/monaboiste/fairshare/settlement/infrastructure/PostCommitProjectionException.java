package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public final class PostCommitProjectionException extends RuntimeException {
    private final long committedVersion;

    public PostCommitProjectionException(SettlementId id, long committedVersion, Throwable cause) {
        super("Settlement " + id + " committed at version " + committedVersion + " but publication failed", cause);
        this.committedVersion = committedVersion;
    }

    public long committedVersion() {
        return committedVersion;
    }
}

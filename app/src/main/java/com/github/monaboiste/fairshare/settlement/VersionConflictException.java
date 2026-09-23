package com.github.monaboiste.fairshare.settlement;

public final class VersionConflictException extends RuntimeException {
    public VersionConflictException(SettlementId id, long expected, long actual) {
        super("Settlement " + id + " expected version " + expected + " but was " + actual);
    }
}

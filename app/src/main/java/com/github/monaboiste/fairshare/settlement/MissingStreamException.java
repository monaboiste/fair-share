package com.github.monaboiste.fairshare.settlement;

public final class MissingStreamException extends RuntimeException {
    public MissingStreamException(SettlementId id) {
        super("Missing Settlement stream: " + id);
    }
}

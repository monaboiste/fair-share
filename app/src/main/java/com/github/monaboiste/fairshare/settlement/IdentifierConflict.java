package com.github.monaboiste.fairshare.settlement;

public record IdentifierConflict(SettlementId id) implements SettlementCommandFailure {}

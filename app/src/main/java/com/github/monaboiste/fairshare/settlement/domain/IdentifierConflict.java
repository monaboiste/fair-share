package com.github.monaboiste.fairshare.settlement.domain;

public record IdentifierConflict(SettlementId id) implements SettlementRejection {}

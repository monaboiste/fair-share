package com.github.monaboiste.fairshare.settlement.domain;

public record SettlementNotFound(SettlementId id) implements SettlementRejection {}

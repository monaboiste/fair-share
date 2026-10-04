package com.github.monaboiste.fairshare.settlement.domain;

public record SettlementIsClosed(SettlementId settlementId) implements SettlementRejection {}

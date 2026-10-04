package com.github.monaboiste.fairshare.settlement.domain;

public record SettlementNotSettled(SettlementId settlementId) implements SettlementRejection {}

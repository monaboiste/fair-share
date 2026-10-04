package com.github.monaboiste.fairshare.settlement.domain;

public record SettlementAlreadyOpen(SettlementId settlementId) implements SettlementRejection {}

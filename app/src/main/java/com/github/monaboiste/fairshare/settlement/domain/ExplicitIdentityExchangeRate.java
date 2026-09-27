package com.github.monaboiste.fairshare.settlement.domain;

public record ExplicitIdentityExchangeRate(SettlementId settlementId) implements SettlementRejection {}

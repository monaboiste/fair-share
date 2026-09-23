package com.github.monaboiste.fairshare.settlement;

public record GetSettlement(SettlementId id) implements SettlementQuery<SettlementView> {}

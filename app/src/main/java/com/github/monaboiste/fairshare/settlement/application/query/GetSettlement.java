package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record GetSettlement(SettlementId id) implements SettlementQuery<SettlementView> {}

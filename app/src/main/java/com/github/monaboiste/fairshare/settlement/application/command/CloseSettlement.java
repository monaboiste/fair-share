package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record CloseSettlement(SettlementId settlementId) implements SettlementCommand {}

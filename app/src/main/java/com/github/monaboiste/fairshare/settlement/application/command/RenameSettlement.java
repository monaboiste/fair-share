package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;

public record RenameSettlement(SettlementId id, SettlementName name) implements SettlementCommand {}

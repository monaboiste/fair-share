package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record RenameSettlement(SettlementId id, String name)
        implements SettlementCommand<Result<SettlementNotFound, SettlementId>> {
    public RenameSettlement {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

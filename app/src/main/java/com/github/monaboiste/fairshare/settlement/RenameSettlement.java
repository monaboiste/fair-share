package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.Result;

public record RenameSettlement(SettlementId id, String name) implements SettlementCommand<Result<Void, SettlementId>> {
    public RenameSettlement {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

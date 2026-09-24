package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import org.jspecify.annotations.Nullable;

public record RenameSettlement(
        SettlementId id, SettlementName name, @Nullable Long expectedVersion) implements SettlementCommand {
    public RenameSettlement(SettlementId id, SettlementName name) {
        this(id, name, null);
    }
}

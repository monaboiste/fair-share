package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import org.jspecify.annotations.Nullable;

public record RenameSettlement(
        SettlementId id, SettlementName name, @Nullable Long expectedVersion)
        implements SettlementCommand<SettlementNotFound, CommitResult<SettlementId, SettlementEvent>> {
    public RenameSettlement(SettlementId id, SettlementName name) {
        this(id, name, null);
    }
}

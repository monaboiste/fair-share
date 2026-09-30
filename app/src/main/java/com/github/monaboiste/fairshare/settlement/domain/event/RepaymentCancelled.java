package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.settlement.domain.RepaymentId;

public record RepaymentCancelled(RepaymentId repaymentId) implements SettlementEvent {
    @Override
    public String type() {
        return "RepaymentCancelled";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

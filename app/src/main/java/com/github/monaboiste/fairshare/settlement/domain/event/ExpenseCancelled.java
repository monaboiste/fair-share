package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.settlement.domain.ExpenseId;

public record ExpenseCancelled(ExpenseId expenseId) implements SettlementEvent {
    @Override
    public String type() {
        return "ExpenseCancelled";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.ExpenseId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record CancelExpense(SettlementId settlementId, ExpenseId expenseId) implements SettlementCommand {}

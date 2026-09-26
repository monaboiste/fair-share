package com.github.monaboiste.fairshare.settlement.domain;

public record ExpenseIdentifierConflict(SettlementId settlementId, ExpenseId expenseId)
        implements SettlementRejection {}

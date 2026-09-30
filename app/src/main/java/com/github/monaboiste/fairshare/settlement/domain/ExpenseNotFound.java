package com.github.monaboiste.fairshare.settlement.domain;

public record ExpenseNotFound(SettlementId settlementId, ExpenseId expenseId) implements SettlementRejection {}

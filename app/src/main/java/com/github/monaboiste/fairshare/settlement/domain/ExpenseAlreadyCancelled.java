package com.github.monaboiste.fairshare.settlement.domain;

public record ExpenseAlreadyCancelled(SettlementId settlementId, ExpenseId expenseId) implements SettlementRejection {}

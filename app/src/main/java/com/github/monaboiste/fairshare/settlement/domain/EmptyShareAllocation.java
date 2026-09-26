package com.github.monaboiste.fairshare.settlement.domain;

public record EmptyShareAllocation(SettlementId settlementId, ExpenseId expenseId) implements SettlementRejection {}

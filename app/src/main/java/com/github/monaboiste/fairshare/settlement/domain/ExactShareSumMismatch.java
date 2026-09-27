package com.github.monaboiste.fairshare.settlement.domain;

public record ExactShareSumMismatch(SettlementId settlementId, ExpenseId expenseId) implements SettlementRejection {}

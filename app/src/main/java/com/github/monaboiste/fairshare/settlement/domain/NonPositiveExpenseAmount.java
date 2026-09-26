package com.github.monaboiste.fairshare.settlement.domain;

public record NonPositiveExpenseAmount(SettlementId settlementId, ExpenseId expenseId) implements SettlementRejection {}

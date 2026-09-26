package com.github.monaboiste.fairshare.settlement.domain;

public record MissingExchangeRate(SettlementId settlementId, ExpenseId expenseId) implements SettlementRejection {}

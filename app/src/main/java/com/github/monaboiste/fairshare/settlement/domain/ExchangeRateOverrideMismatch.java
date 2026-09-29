package com.github.monaboiste.fairshare.settlement.domain;

public record ExchangeRateOverrideMismatch(SettlementId settlementId, ExpenseId expenseId)
        implements SettlementRejection {}

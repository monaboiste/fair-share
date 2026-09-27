package com.github.monaboiste.fairshare.settlement.domain;

public record ExactShareCurrencyMismatch(SettlementId settlementId, ExpenseId expenseId, ParticipantId participantId)
        implements SettlementRejection {}

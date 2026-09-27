package com.github.monaboiste.fairshare.settlement.domain;

public record NonPositiveShareWeight(SettlementId settlementId, ExpenseId expenseId, ParticipantId participantId)
        implements SettlementRejection {}

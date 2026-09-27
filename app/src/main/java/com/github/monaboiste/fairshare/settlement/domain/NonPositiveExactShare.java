package com.github.monaboiste.fairshare.settlement.domain;

public record NonPositiveExactShare(SettlementId settlementId, ExpenseId expenseId, ParticipantId participantId)
        implements SettlementRejection {}

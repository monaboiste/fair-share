package com.github.monaboiste.fairshare.settlement.domain;

public sealed interface SettlementRejection
        permits SettlementNotFound,
                ParticipantIdentifierConflict,
                ParticipantNotFound,
                EmptyShareAllocation,
                NonPositiveExpenseAmount,
                ExpenseIdentifierConflict,
                ParticipantReferenced,
                MissingExchangeRate {}

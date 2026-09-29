package com.github.monaboiste.fairshare.settlement.domain;

public sealed interface SettlementRejection
        permits SettlementNotFound,
                ParticipantIdentifierConflict,
                ParticipantNotFound,
                EmptyShareAllocation,
                NonPositiveExpenseAmount,
                NonPositiveRepaymentAmount,
                RepaymentIdentifierConflict,
                RepaymentCurrencyMismatch,
                RepaymentAmountPrecisionExceeded,
                SelfDirectedRepayment,
                ExpenseIdentifierConflict,
                ParticipantReferenced,
                MissingExchangeRate,
                ExactShareCurrencyMismatch,
                NonPositiveExactShare,
                ExactShareSumMismatch,
                NonPositiveShareWeight,
                ExchangeRateTargetMismatch,
                ExplicitIdentityExchangeRate,
                ExchangeRateOverrideMismatch {}

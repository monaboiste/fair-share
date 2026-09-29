package com.github.monaboiste.fairshare.settlement.domain;

public record RepaymentCurrencyMismatch(SettlementId settlementId, RepaymentId repaymentId)
        implements SettlementRejection {}

package com.github.monaboiste.fairshare.settlement.domain;

public record RepaymentAmountPrecisionExceeded(SettlementId settlementId, RepaymentId repaymentId)
        implements SettlementRejection {}

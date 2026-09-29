package com.github.monaboiste.fairshare.settlement.domain;

public record NonPositiveRepaymentAmount(SettlementId settlementId, RepaymentId repaymentId)
        implements SettlementRejection {}

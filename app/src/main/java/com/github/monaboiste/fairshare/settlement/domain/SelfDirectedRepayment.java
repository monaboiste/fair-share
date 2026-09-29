package com.github.monaboiste.fairshare.settlement.domain;

public record SelfDirectedRepayment(SettlementId settlementId, RepaymentId repaymentId)
        implements SettlementRejection {}

package com.github.monaboiste.fairshare.settlement.domain;

public record RepaymentAlreadyCancelled(SettlementId settlementId, RepaymentId repaymentId)
        implements SettlementRejection {}

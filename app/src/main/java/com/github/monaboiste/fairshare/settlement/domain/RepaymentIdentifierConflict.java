package com.github.monaboiste.fairshare.settlement.domain;

public record RepaymentIdentifierConflict(SettlementId settlementId, RepaymentId repaymentId)
        implements SettlementRejection {}

package com.github.monaboiste.fairshare.settlement.domain;

public record RepaymentNotFound(SettlementId settlementId, RepaymentId repaymentId) implements SettlementRejection {}

package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.RepaymentId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record CancelRepayment(SettlementId settlementId, RepaymentId repaymentId) implements SettlementCommand {}

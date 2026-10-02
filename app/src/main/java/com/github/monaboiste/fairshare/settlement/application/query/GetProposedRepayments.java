package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record GetProposedRepayments(SettlementId id) implements SettlementQuery<ProposedRepaymentGraphView> {}

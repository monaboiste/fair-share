package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.netting.ProposedRepayment;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import java.util.List;
import javax.money.CurrencyUnit;

public record ProposedRepaymentGraphView(
        SettlementId id,
        CurrencyUnit currency,
        long version,
        List<ParticipantView> participants,
        List<ProposedRepayment<ParticipantId>> proposedRepayments) {
    public ProposedRepaymentGraphView {
        participants = List.copyOf(participants);
        proposedRepayments = List.copyOf(proposedRepayments);
    }
}

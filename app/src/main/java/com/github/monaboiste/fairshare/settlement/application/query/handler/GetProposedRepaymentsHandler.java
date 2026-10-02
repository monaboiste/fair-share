package com.github.monaboiste.fairshare.settlement.application.query.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.queries.QueryHandler;
import com.github.monaboiste.fairshare.netting.Netting;
import com.github.monaboiste.fairshare.netting.Obligations;
import com.github.monaboiste.fairshare.netting.ParticipantComparator;
import com.github.monaboiste.fairshare.netting.ProposedRepayment;
import com.github.monaboiste.fairshare.settlement.application.query.GetProposedRepayments;
import com.github.monaboiste.fairshare.settlement.application.query.ParticipantView;
import com.github.monaboiste.fairshare.settlement.application.query.ProposedRepaymentGraphView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementViews;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import java.util.Comparator;
import java.util.stream.Collectors;

public final class GetProposedRepaymentsHandler
        implements QueryHandler<GetProposedRepayments, SettlementRejection, ProposedRepaymentGraphView> {
    private static final ParticipantComparator<ParticipantId> PARTICIPANT_ORDER =
            (left, right) -> left.value().compareTo(right.value());

    private final SettlementViews views;

    public GetProposedRepaymentsHandler(SettlementViews views) {
        this.views = views;
    }

    @Override
    public Result<SettlementRejection, ProposedRepaymentGraphView> handle(GetProposedRepayments query) {
        return views.findById(query.id())
                .map(GetProposedRepaymentsHandler::propose)
                .map(Result::<SettlementRejection, ProposedRepaymentGraphView>success)
                .orElseGet(() -> Result.failure(new SettlementNotFound(query.id())));
    }

    private static ProposedRepaymentGraphView propose(SettlementView view) {
        var participants = view.participants().stream()
                .sorted(Comparator.comparing(ParticipantView::id, PARTICIPANT_ORDER))
                .toList();
        var roster = participants.stream().map(ParticipantView::id).collect(Collectors.toSet());
        var obligations = Obligations.of(roster, view.obligations(), view.currency());
        var repayments = Netting.greedy().net(obligations, PARTICIPANT_ORDER).proposedRepayments().stream()
                .sorted(Comparator.comparing(ProposedRepayment<ParticipantId>::debtor, PARTICIPANT_ORDER)
                        .thenComparing(ProposedRepayment::creditor, PARTICIPANT_ORDER))
                .toList();
        return new ProposedRepaymentGraphView(view.id(), view.currency(), view.version(), participants, repayments);
    }
}

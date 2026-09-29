package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.application.command.RecordRepayment;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentDetails;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository;

/**
 * Records an actual Repayment in an existing Settlement, independently of Proposed Repayments.
 *
 * <p>The caller supplies a Repayment identifier unique within the Settlement. Every reuse, including an identical
 * submission, returns a typed identifier conflict. A new Repayment must have a positive amount in the Settlement
 * Currency, exactly representable in its minor units, and distinct active Participants. Invalid Repayments return typed
 * rejections without committing; an unknown Settlement returns {@link SettlementNotFound}.
 *
 * <p>The business date is unrestricted and separate from the event's registration time. Overpayment is allowed.
 */
public final class RecordRepaymentHandler
        implements CommandHandler<RecordRepayment, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;

    public RecordRepaymentHandler(SettlementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(RecordRepayment command) {
        Settlement settlement = repository.findById(command.settlementId()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.settlementId()));
        }
        var repayment = new RepaymentDetails(
                command.repaymentId(), command.paidOn(), command.payer(), command.recipient(), command.amount());
        var decision = settlement.recordRepayment(repayment);
        if (decision.failure()) {
            return Result.failure(decision.getFailure());
        }
        return Result.success(repository.save(settlement));
    }
}

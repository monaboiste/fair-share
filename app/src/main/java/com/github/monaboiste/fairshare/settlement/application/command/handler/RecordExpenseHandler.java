package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;

public final class RecordExpenseHandler
        implements CommandHandler<RecordExpense, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;

    public RecordExpenseHandler(SettlementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(RecordExpense command) {
        Settlement settlement = repository.findById(command.settlementId()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.settlementId()));
        }
        var decision = settlement.recordExpense(
                command.expenseId(),
                command.description(),
                command.incurredOn(),
                command.payer(),
                command.amount(),
                command.allocation());
        if (decision.failure()) {
            return Result.failure(decision.getFailure());
        }
        return Result.success(repository.save(settlement));
    }
}

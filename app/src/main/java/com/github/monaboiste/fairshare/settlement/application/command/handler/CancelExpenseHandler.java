package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.application.command.CancelExpense;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository;

public final class CancelExpenseHandler
        implements CommandHandler<CancelExpense, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;

    public CancelExpenseHandler(SettlementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(CancelExpense command) {
        Settlement settlement = repository.findById(command.settlementId()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.settlementId()));
        }
        var decision = settlement.cancelExpense(command.expenseId());
        if (decision.failure()) {
            return Result.failure(decision.getFailure());
        }
        return Result.success(repository.save(settlement));
    }
}

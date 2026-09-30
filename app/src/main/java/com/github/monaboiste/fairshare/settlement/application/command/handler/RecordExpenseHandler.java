package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDetails;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository;
import com.github.monaboiste.fairshare.valuation.ExchangeRateOverride;
import com.github.monaboiste.fairshare.valuation.ValuationEngine;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Records a valued Expense in an existing Settlement.
 *
 * <p>Every reused Expense identifier rejects. Invalid Expenses return typed rejections, and an unknown Settlement
 * returns {@link SettlementNotFound}. Concurrent commits throw {@link VersionConflictException}.
 */
public final class RecordExpenseHandler
        implements CommandHandler<RecordExpense, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final Clock clock;
    private final ValuationEngine engine;
    private final Supplier<ComponentVersionId> versionIds;

    public RecordExpenseHandler(SettlementRepository repository, Clock clock) {
        this(repository, clock, ValuationEngine.standard(), ComponentVersionId::generate);
    }

    public RecordExpenseHandler(
            SettlementRepository repository,
            Clock clock,
            ValuationEngine engine,
            Supplier<ComponentVersionId> versionIds) {
        this.repository = repository;
        this.clock = clock;
        this.engine = engine;
        this.versionIds = versionIds;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(RecordExpense command) {
        Settlement settlement = repository.findById(command.settlementId()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.settlementId()));
        }
        ExchangeRateOverride override = command.exchangeRateOverride() == null
                ? null
                : new ExchangeRateOverride(command.exchangeRateOverride(), versionIds.get(), LocalDateTime.now(clock));
        var expense = new ExpenseDetails(
                command.expenseId(),
                command.description(),
                command.incurredOn(),
                command.payer(),
                command.amount(),
                command.allocation());
        var decision = settlement.recordExpense(expense, override, engine);
        if (decision.failure()) {
            return Result.failure(decision.getFailure());
        }
        return Result.success(repository.save(settlement));
    }
}

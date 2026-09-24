package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.time.Clock;

/**
 * Opens a Settlement with a generated identifier.
 *
 * <p>Succeeds with the generated identifier as {@code CommitResult.streamId()}, the committed {@code SettlementOpened}
 * envelope, and stream version 1. Store and post-commit publication failures propagate as exceptions.
 */
public final class OpenSettlementHandler
        implements CommandHandler<OpenSettlement, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final Clock clock;

    public OpenSettlementHandler(SettlementRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(OpenSettlement command) {
        Settlement settlement = Settlement.open(command.name(), command.currency(), clock);
        CommitResult<SettlementId, SettlementEvent> result = repository.save(settlement);
        return Result.success(result);
    }
}

package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;

/**
 * Changes the display name of an existing Settlement; the Settlement Currency never changes.
 *
 * <p>Succeeds with the committed {@code SettlementRenamed} envelope and the new stream version. Renaming to the current
 * name is a no-op that succeeds with no envelopes and the unchanged version. Rejects with {@link SettlementNotFound}
 * when the Settlement does not exist. A concurrent commit between loading and saving throws
 * {@link VersionConflictException}. Nothing is retried. Store and post-commit publication failures propagate as
 * exceptions.
 */
public final class RenameSettlementHandler
        implements CommandHandler<RenameSettlement, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;

    public RenameSettlementHandler(SettlementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(RenameSettlement command) {
        Settlement settlement = repository.findById(command.id()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.id()));
        }
        settlement.rename(command.name());
        CommitResult<SettlementId, SettlementEvent> result = repository.save(settlement);
        return Result.success(result);
    }
}

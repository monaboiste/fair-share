package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.time.Clock;

/**
 * Changes the display name of an existing Settlement; the Settlement Currency never changes.
 *
 * <p>Succeeds with the committed {@code SettlementRenamed} envelope and the new stream version. Renaming to the current
 * name is a no-op that succeeds with no envelopes and the unchanged version. Rejects with {@link SettlementNotFound}
 * when the Settlement does not exist. When the command carries an expected version that differs from the stored
 * version, throws {@link VersionConflictException} before deciding, even for a no-op; a concurrent commit between
 * loading and saving throws the same exception. Nothing is retried. Store and post-commit publication failures
 * propagate as exceptions.
 */
public final class RenameSettlementHandler
        implements CommandHandler<RenameSettlement, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final Clock clock;

    public RenameSettlementHandler(SettlementRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(RenameSettlement command) {
        return repository
                .findById(command.id())
                .map(settlement -> {
                    Long expected = command.expectedVersion();
                    if (expected != null && expected != settlement.committedVersion()) {
                        throw new VersionConflictException(command.id(), expected, settlement.committedVersion());
                    }
                    settlement.rename(command.name(), clock.instant());
                    return Result.<SettlementRejection, CommitResult<SettlementId, SettlementEvent>>success(
                            repository.save(settlement));
                })
                .orElseGet(() -> Result.failure(new SettlementNotFound(command.id())));
    }
}

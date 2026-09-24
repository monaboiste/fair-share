package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.time.Clock;
import java.util.function.Supplier;

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
    private final Supplier<EventId> eventIds;
    private final Clock clock;

    public RenameSettlementHandler(SettlementRepository repository, Supplier<EventId> eventIds, Clock clock) {
        this.repository = repository;
        this.eventIds = eventIds;
        this.clock = clock;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(RenameSettlement command) {
        return repository
                .findById(command.id())
                .map(settlement -> {
                    settlement.rename(command.name(), eventIds.get(), clock.instant());
                    return Result.<SettlementRejection, CommitResult<SettlementId, SettlementEvent>>success(
                            repository.save(settlement));
                })
                .orElseGet(() -> Result.failure(new SettlementNotFound(command.id())));
    }
}

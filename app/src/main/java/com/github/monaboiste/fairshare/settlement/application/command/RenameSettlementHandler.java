package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.time.Clock;

public final class RenameSettlementHandler
        implements CommandHandler<RenameSettlement, SettlementNotFound, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final Clock clock;

    public RenameSettlementHandler(SettlementRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Result<SettlementNotFound, CommitResult<SettlementId, SettlementEvent>> handle(RenameSettlement command) {
        return repository
                .findById(command.id())
                .map(settlement -> {
                    if (command.expectedVersion() != null
                            && command.expectedVersion() != settlement.committedVersion()) {
                        throw new VersionConflictException(
                                command.id(), command.expectedVersion(), settlement.committedVersion());
                    }
                    settlement.rename(command.name(), clock.instant());
                    return Result.<SettlementNotFound, CommitResult<SettlementId, SettlementEvent>>success(
                            repository.save(settlement));
                })
                .orElseGet(() -> Result.failure(new SettlementNotFound(command.id())));
    }
}

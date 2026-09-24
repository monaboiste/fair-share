package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRepository;
import java.time.Clock;

public final class RenameSettlementHandler
        implements CommandHandler<RenameSettlement, Result<SettlementNotFound, SettlementId>> {
    private final SettlementRepository repository;
    private final Clock clock;

    public RenameSettlementHandler(SettlementRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Class<RenameSettlement> commandType() {
        return RenameSettlement.class;
    }

    @Override
    public Result<SettlementNotFound, SettlementId> handle(RenameSettlement command) {
        return repository
                .findById(command.id())
                .map(settlement -> {
                    settlement.rename(command.name(), clock.instant());
                    repository.save(settlement);
                    return Result.<SettlementNotFound, SettlementId>success(command.id());
                })
                .orElseGet(() -> Result.failure(new SettlementNotFound(command.id())));
    }
}

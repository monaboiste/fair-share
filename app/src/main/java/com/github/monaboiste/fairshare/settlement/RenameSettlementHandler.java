package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.CommandHandler;
import com.github.monaboiste.fairshare.common.Result;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public final class RenameSettlementHandler implements CommandHandler<RenameSettlement, Result<Void, SettlementId>> {
    private final SourcedSettlementRepository repository;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public RenameSettlementHandler(SourcedSettlementRepository repository, Clock clock, Supplier<UUID> eventIds) {
        this.repository = repository;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    @Override
    public Class<RenameSettlement> commandType() {
        return RenameSettlement.class;
    }

    @Override
    public Result<Void, SettlementId> handle(RenameSettlement command) {
        return repository.withLock(command.id(), () -> {
            Settlement settlement = repository.load(command.id());
            settlement.rename(command.name(), clock, eventIds);
            repository.save(settlement);
            return Result.success(command.id());
        });
    }
}

package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.CommandHandler;
import com.github.monaboiste.fairshare.common.Result;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public final class OpenSettlementHandler
        implements CommandHandler<OpenSettlement, Result<IdentifierConflict, SettlementId>> {
    private final SourcedSettlementRepository repository;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public OpenSettlementHandler(SourcedSettlementRepository repository, Clock clock, Supplier<UUID> eventIds) {
        this.repository = repository;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    @Override
    public Class<OpenSettlement> commandType() {
        return OpenSettlement.class;
    }

    @Override
    public Result<IdentifierConflict, SettlementId> handle(OpenSettlement command) {
        return repository.withLock(command.id(), () -> {
            Settlement settlement = repository.loadOrCreate(command.id());
            if (!settlement.open(command.name(), command.currency(), clock, eventIds)) {
                return Result.failure(new IdentifierConflict(command.id()));
            }
            repository.save(settlement);
            return Result.success(command.id());
        });
    }
}

package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.settlement.domain.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRepository;
import java.time.Clock;

public final class OpenSettlementHandler
        implements CommandHandler<OpenSettlement, Result<IdentifierConflict, SettlementId>> {
    private final SettlementRepository repository;
    private final Clock clock;

    public OpenSettlementHandler(SettlementRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Class<OpenSettlement> commandType() {
        return OpenSettlement.class;
    }

    @Override
    public Result<IdentifierConflict, SettlementId> handle(OpenSettlement command) {
        return repository
                .findById(command.id())
                .map(existing -> existing.isOpenedWith(command.name(), command.currency())
                        ? Result.<IdentifierConflict, SettlementId>success(command.id())
                        : Result.<IdentifierConflict, SettlementId>failure(new IdentifierConflict(command.id())))
                .orElseGet(() -> {
                    repository.save(Settlement.open(command.id(), command.name(), command.currency(), clock.instant()));
                    return Result.success(command.id());
                });
    }
}

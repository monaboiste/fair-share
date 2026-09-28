package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.Supplier;

public final class ConfigureExchangeRateHandler
        implements CommandHandler<
                ConfigureExchangeRate, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final Clock clock;
    private final Supplier<ComponentVersionId> versionIds;

    public ConfigureExchangeRateHandler(SettlementRepository repository, Clock clock) {
        this(repository, clock, ComponentVersionId::generate);
    }

    public ConfigureExchangeRateHandler(
            SettlementRepository repository, Clock clock, Supplier<ComponentVersionId> versionIds) {
        this.repository = repository;
        this.clock = clock;
        this.versionIds = versionIds;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(
            ConfigureExchangeRate command) {
        Settlement settlement = repository.findById(command.settlementId()).orElse(null);
        if (settlement == null) {
            return Result.failure(new SettlementNotFound(command.settlementId()));
        }
        var decision = settlement.configureExchangeRate(
                command.exchangeRate(), command.validity(), versionIds.get(), LocalDateTime.now(clock));
        if (decision.failure()) {
            return Result.failure(decision.getFailure());
        }
        return Result.success(repository.save(settlement));
    }
}

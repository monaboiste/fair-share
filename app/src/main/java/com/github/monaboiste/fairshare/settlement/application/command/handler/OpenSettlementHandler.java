package com.github.monaboiste.fairshare.settlement.application.command.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement;
import com.github.monaboiste.fairshare.settlement.domain.IdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.time.Clock;
import java.util.function.Supplier;

/**
 * Opens a new Settlement under the caller-supplied identifier.
 *
 * <p>Succeeds with the committed {@code SettlementOpened} envelope and stream version 1. Rejects with
 * {@link IdentifierConflict} when a Settlement with the identifier already exists, whatever its details; opening is not
 * idempotent and is never retried. A concurrent open of the same identifier that commits first makes this one fail with
 * {@link com.github.monaboiste.fairshare.common.events.VersionConflictException}. Store and post-commit publication
 * failures propagate as exceptions.
 */
public final class OpenSettlementHandler
        implements CommandHandler<OpenSettlement, SettlementRejection, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final Supplier<EventId> eventIds;
    private final Clock clock;

    public OpenSettlementHandler(SettlementRepository repository, Supplier<EventId> eventIds, Clock clock) {
        this.repository = repository;
        this.eventIds = eventIds;
        this.clock = clock;
    }

    @Override
    public Result<SettlementRejection, CommitResult<SettlementId, SettlementEvent>> handle(OpenSettlement command) {
        if (repository.findById(command.id()).isPresent()) {
            return Result.failure(new IdentifierConflict(command.id()));
        }
        return Result.success(repository.save(
                Settlement.open(command.id(), command.name(), command.currency(), eventIds.get(), clock.instant())));
    }
}

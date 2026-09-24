package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.commands.CommandHandler;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStreamReader;
import com.github.monaboiste.fairshare.common.events.VersionConflictException;
import com.github.monaboiste.fairshare.settlement.domain.IdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.time.Clock;
import java.util.List;

public final class OpenSettlementHandler
        implements CommandHandler<OpenSettlement, IdentifierConflict, CommitResult<SettlementId, SettlementEvent>> {
    private final SettlementRepository repository;
    private final EventStreamReader<SettlementId, SettlementEvent> streams;
    private final Clock clock;

    public OpenSettlementHandler(
            SettlementRepository repository, EventStreamReader<SettlementId, SettlementEvent> streams, Clock clock) {
        this.repository = repository;
        this.streams = streams;
        this.clock = clock;
    }

    @Override
    public Result<IdentifierConflict, CommitResult<SettlementId, SettlementEvent>> handle(OpenSettlement command) {
        var existing = repository.findById(command.id());
        if (existing.isPresent()) {
            return retry(command, existing.get());
        }
        try {
            return Result.success(repository.save(
                    Settlement.open(command.id(), command.name(), command.currency(), clock.instant())));
        } catch (VersionConflictException conflict) {
            return repository
                    .findById(command.id())
                    .map(aggregate -> retry(command, aggregate))
                    .orElseThrow(() -> conflict);
        }
    }

    private Result<IdentifierConflict, CommitResult<SettlementId, SettlementEvent>> retry(
            OpenSettlement command, Settlement existing) {
        return existing.acceptOpeningRetry(command.name(), command.currency()).map(_ -> {
            EventEnvelope<SettlementId, SettlementEvent> opened =
                    streams.load(command.id()).getFirst();
            return new CommitResult<>(List.of(opened), opened.sequence());
        });
    }
}

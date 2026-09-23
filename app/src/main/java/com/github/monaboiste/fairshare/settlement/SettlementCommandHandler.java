package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.events.AppendResult;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.MissingStreamException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class SettlementCommandHandler {
    private final EventStore<SettlementId, SettlementEvent> store;
    private final SettlementQueryHandler queries;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public SettlementCommandHandler(
            EventStore<SettlementId, SettlementEvent> store, Clock clock, Supplier<UUID> eventIds) {
        this.store = store;
        this.queries = new SettlementQueryHandler(store);
        this.clock = clock;
        this.eventIds = eventIds;
    }

    public Result<SettlementCommandFailure, AppendResult<SettlementId, SettlementEvent>> handle(
            OpenSettlement command) {
        SettlementOpened opening = new SettlementOpened(command.name(), command.currency());
        List<EventEnvelope<SettlementId, SettlementEvent>> existing;
        try {
            existing = queries.handle(new GetSettlementHistory(command.id()));
        } catch (MissingStreamException missing) {
            EventEnvelope<SettlementId, SettlementEvent> event = envelope(command.id(), 1, opening);
            return Result.success(store.append(command.id(), 0, List.of(event)));
        }
        if (existing.getFirst().payload().equals(opening)) {
            return Result.success(new AppendResult<>(List.of(existing.getFirst()), 1));
        }
        return Result.failure(new IdentifierConflict(command.id()));
    }

    public Result<SettlementCommandFailure, AppendResult<SettlementId, SettlementEvent>> handle(
            RenameSettlement command) {
        SettlementView current = queries.handle(new GetSettlement(command.id()));
        if (current.name().equals(command.name())) {
            return Result.success(new AppendResult<>(List.of(), current.version()));
        }
        SettlementRenamed renaming = new SettlementRenamed(command.name());
        return Result.success(store.append(
                command.id(), current.version(), List.of(envelope(command.id(), current.version() + 1, renaming))));
    }

    private EventEnvelope<SettlementId, SettlementEvent> envelope(
            SettlementId id, long sequence, SettlementEvent event) {
        return new EventEnvelope<>(eventIds.get(), id, sequence, clock.instant(), event);
    }
}

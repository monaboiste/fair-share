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
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public SettlementCommandHandler(
            EventStore<SettlementId, SettlementEvent> store, Clock clock, Supplier<UUID> eventIds) {
        this.store = store;
        this.clock = clock;
        this.eventIds = eventIds;
    }

    public Result<SettlementCommandFailure, AppendResult<SettlementId, SettlementEvent>> handle(
            OpenSettlement command) {
        SettlementOpened opening = new SettlementOpened(command.name(), command.currency());
        List<EventEnvelope<SettlementId, SettlementEvent>> existing;
        try {
            existing = store.load(command.id());
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
        List<EventEnvelope<SettlementId, SettlementEvent>> events = store.load(command.id());
        long version = events.size();
        if (currentName(events).equals(command.name())) {
            return Result.success(new AppendResult<>(List.of(), version));
        }
        SettlementRenamed renaming = new SettlementRenamed(command.name());
        return Result.success(
                store.append(command.id(), version, List.of(envelope(command.id(), version + 1, renaming))));
    }

    private static String currentName(List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        return switch (events.getLast().payload()) {
            case SettlementOpened opened -> opened.name();
            case SettlementRenamed renamed -> renamed.name();
        };
    }

    private EventEnvelope<SettlementId, SettlementEvent> envelope(
            SettlementId id, long sequence, SettlementEvent event) {
        return new EventEnvelope<>(eventIds.get(), id, sequence, clock.instant(), event);
    }
}

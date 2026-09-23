package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.Result;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.money.CurrencyUnit;

public final class SettlementCommands {
    private final EventStore store;
    private final Clock clock;
    private final Supplier<UUID> eventIds;

    public SettlementCommands(EventStore store, Clock clock, Supplier<UUID> eventIds) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        this.eventIds = Objects.requireNonNull(eventIds);
    }

    public Result<IdentifierConflict, AppendResult> open(SettlementId id, String name, CurrencyUnit currency) {
        SettlementOpened opening = new SettlementOpened(name, currency);
        List<EventEnvelope> existing;
        try {
            existing = store.load(id);
        } catch (MissingStreamException missing) {
            EventEnvelope event = envelope(id, 1, opening);
            return Result.success(store.append(id, 0, List.of(event)));
        }
        if (existing.getFirst().payload().equals(opening)) {
            return Result.success(new AppendResult(List.of(existing.getFirst()), 1));
        }
        return Result.failure(new IdentifierConflict(id));
    }

    public AppendResult rename(SettlementId id, String name) {
        SettlementRenamed renaming = new SettlementRenamed(name);
        SettlementView current = view(id);
        if (current.name().equals(name)) {
            return new AppendResult(List.of(), current.version());
        }
        return store.append(id, current.version(), List.of(envelope(id, current.version() + 1, renaming)));
    }

    public SettlementView view(SettlementId id) {
        List<EventEnvelope> events = history(id);
        SettlementOpened opening = (SettlementOpened) events.getFirst().payload();
        String name = opening.name();
        for (EventEnvelope event : events) {
            if (event.payload() instanceof SettlementRenamed renamed) {
                name = renamed.name();
            }
        }
        return new SettlementView(id, name, opening.currency(), events.size());
    }

    public List<EventEnvelope> history(SettlementId id) {
        return List.copyOf(store.load(id));
    }

    private EventEnvelope envelope(SettlementId id, long sequence, SettlementEvent event) {
        String type = event instanceof SettlementOpened ? "SettlementOpened" : "SettlementRenamed";
        return new EventEnvelope(eventIds.get(), id, sequence, clock.instant(), type, 1, event);
    }
}

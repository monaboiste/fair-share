package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.settlement.domain.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class EventSourcedSettlementRepository implements SettlementRepository {
    private final EventStore<SettlementId, SettlementEvent> store;
    private final Supplier<EventId> eventIds;
    private final Consumer<List<EventEnvelope<SettlementId, SettlementEvent>>> onCommitted;

    public EventSourcedSettlementRepository(
            EventStore<SettlementId, SettlementEvent> store,
            Supplier<EventId> eventIds,
            Consumer<List<EventEnvelope<SettlementId, SettlementEvent>>> onCommitted) {
        this.store = store;
        this.eventIds = eventIds;
        this.onCommitted = onCommitted;
    }

    @Override
    public Optional<Settlement> findById(SettlementId id) {
        List<EventEnvelope<SettlementId, SettlementEvent>> history = store.load(id);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Settlement.recreate(
                id, history.stream().map(EventEnvelope::payload).toList()));
    }

    @Override
    public void save(Settlement settlement) {
        List<SettlementEvent> pending = settlement.pendingEvents();
        if (pending.isEmpty()) {
            return;
        }
        long expected = settlement.committedVersion();
        List<EventEnvelope<SettlementId, SettlementEvent>> envelopes = IntStream.range(0, pending.size())
                .mapToObj(index ->
                        new EventEnvelope<>(eventIds.get(), settlement.id(), expected + index + 1, pending.get(index)))
                .toList();
        var committed = store.append(settlement.id(), expected, envelopes);
        settlement.flushPendingEvents();
        try {
            onCommitted.accept(committed.events());
        } catch (RuntimeException e) {
            System.getLogger(getClass().getName())
                    .log(
                            System.Logger.Level.ERROR,
                            "Fatal post-commit Settlement publication failure at version " + committed.version(),
                            e);
            throw new PostCommitProjectionException(settlement.id(), committed.version(), e);
        }
    }
}

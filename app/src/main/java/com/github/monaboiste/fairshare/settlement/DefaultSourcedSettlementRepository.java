package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.MissingStreamException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DefaultSourcedSettlementRepository implements SourcedSettlementRepository {
    private final EventStore<SettlementId, SettlementEvent> store;
    private final CommittedSettlementEvents publisher;
    private final ConcurrentHashMap<SettlementId, Object> locks = new ConcurrentHashMap<>();

    public DefaultSourcedSettlementRepository(
            EventStore<SettlementId, SettlementEvent> store, CommittedSettlementEvents publisher) {
        this.store = store;
        this.publisher = publisher;
    }

    @Override
    public <R> R withLock(SettlementId id, Supplier<R> decision) {
        synchronized (locks.computeIfAbsent(id, ignored -> new Object())) {
            return decision.get();
        }
    }

    @Override
    public Settlement load(SettlementId id) {
        return Settlement.replay(id, store.load(id));
    }

    @Override
    public Settlement loadOrCreate(SettlementId id) {
        try {
            return load(id);
        } catch (MissingStreamException missing) {
            return new Settlement(id);
        }
    }

    @Override
    public void save(Settlement settlement) {
        var incoming = settlement.incoming();
        if (incoming.isEmpty()) {
            return;
        }
        var committed = store.append(settlement.id(), settlement.loadedVersion(), incoming);
        settlement.committed(committed.version());
        try {
            publisher.publish(committed.events());
        } catch (RuntimeException failure) {
            System.getLogger(getClass().getName())
                    .log(
                            System.Logger.Level.ERROR,
                            "Fatal post-commit Settlement publication failure at version " + committed.version(),
                            failure);
            throw new PostCommitProjectionException(settlement.id(), committed.version(), failure);
        }
    }
}

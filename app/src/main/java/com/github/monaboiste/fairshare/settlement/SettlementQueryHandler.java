package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import java.util.List;

public final class SettlementQueryHandler {
    private final EventStore<SettlementId, SettlementEvent> store;
    private final SettlementProjector projector;

    public SettlementQueryHandler(EventStore<SettlementId, SettlementEvent> store, SettlementProjector projector) {
        this.store = store;
        this.projector = projector;
    }

    public SettlementView handle(GetSettlement query) {
        return projector.get(query.id());
    }

    public List<EventEnvelope<SettlementId, SettlementEvent>> handle(GetSettlementHistory query) {
        return List.copyOf(store.load(query.id()));
    }
}

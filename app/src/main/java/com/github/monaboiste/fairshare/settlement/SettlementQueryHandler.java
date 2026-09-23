package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import java.util.List;

public final class SettlementQueryHandler {
    private final EventStore<SettlementId, SettlementEvent> store;

    public SettlementQueryHandler(EventStore<SettlementId, SettlementEvent> store) {
        this.store = store;
    }

    public SettlementView handle(GetSettlement query) {
        List<EventEnvelope<SettlementId, SettlementEvent>> events = handle(new GetSettlementHistory(query.id()));
        SettlementOpened opening = (SettlementOpened) events.getFirst().payload();
        String name = opening.name();
        for (EventEnvelope<SettlementId, SettlementEvent> event : events) {
            if (event.payload() instanceof SettlementRenamed renamed) {
                name = renamed.name();
            }
        }
        return new SettlementView(query.id(), name, opening.currency(), events.size());
    }

    public List<EventEnvelope<SettlementId, SettlementEvent>> handle(GetSettlementHistory query) {
        return List.copyOf(store.load(query.id()));
    }
}

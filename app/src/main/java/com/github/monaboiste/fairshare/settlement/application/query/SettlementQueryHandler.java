package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.util.List;
import java.util.Optional;

public final class SettlementQueryHandler {
    private final SettlementViews views;
    private final EventStore<SettlementId, SettlementEvent> store;

    public SettlementQueryHandler(SettlementViews views, EventStore<SettlementId, SettlementEvent> store) {
        this.views = views;
        this.store = store;
    }

    public Optional<SettlementView> handle(GetSettlement query) {
        return views.findById(query.id());
    }

    public List<EventEnvelope<SettlementId, SettlementEvent>> handle(GetSettlementHistory query) {
        return List.copyOf(store.load(query.id()));
    }
}

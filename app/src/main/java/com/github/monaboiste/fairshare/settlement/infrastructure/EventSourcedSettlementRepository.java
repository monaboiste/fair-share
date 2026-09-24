package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.eventsourcing.EventSourcedRepository;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.Settlement;
import com.github.monaboiste.fairshare.settlement.domain.aggregate.SettlementRepository;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.util.function.Supplier;

public final class EventSourcedSettlementRepository
        extends EventSourcedRepository<SettlementId, SettlementEvent, Settlement> implements SettlementRepository {
    public EventSourcedSettlementRepository(
            EventStore<SettlementId, SettlementEvent> store, Supplier<EventId> eventIds) {
        super(store, eventIds, Settlement.factory());
    }
}

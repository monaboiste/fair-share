package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStreamReader;
import com.github.monaboiste.fairshare.common.queries.QueryHandler;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.util.List;

public final class GetSettlementHistoryHandler
        implements QueryHandler<GetSettlementHistory, List<EventEnvelope<SettlementId, SettlementEvent>>> {
    private final EventStreamReader<SettlementId, SettlementEvent> streams;

    public GetSettlementHistoryHandler(EventStreamReader<SettlementId, SettlementEvent> streams) {
        this.streams = streams;
    }

    @Override
    public List<EventEnvelope<SettlementId, SettlementEvent>> handle(GetSettlementHistory query) {
        return streams.load(query.id());
    }
}

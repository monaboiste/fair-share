package com.github.monaboiste.fairshare.settlement.application.query.handler;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStreamReader;
import com.github.monaboiste.fairshare.common.queries.QueryHandler;
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.util.List;

/**
 * Reads ordered committed events from a Settlement stream.
 *
 * <p>Succeeds with the stream's envelopes or rejects with {@link SettlementNotFound} when the stream is absent. Store
 * failures, including a stream disappearing between existence check and load, propagate as exceptions.
 */
public final class GetSettlementHistoryHandler
        implements QueryHandler<
                GetSettlementHistory, SettlementRejection, List<EventEnvelope<SettlementId, SettlementEvent>>> {
    private final EventStreamReader<SettlementId, SettlementEvent> streams;

    public GetSettlementHistoryHandler(EventStreamReader<SettlementId, SettlementEvent> streams) {
        this.streams = streams;
    }

    @Override
    public Result<SettlementRejection, List<EventEnvelope<SettlementId, SettlementEvent>>> handle(
            GetSettlementHistory query) {
        if (!streams.exists(query.id())) {
            return Result.failure(new SettlementNotFound(query.id()));
        }
        return Result.success(streams.load(query.id()));
    }
}

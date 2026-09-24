package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import java.util.List;

public record GetSettlementHistory(SettlementId id)
        implements SettlementQuery<List<EventEnvelope<SettlementId, SettlementEvent>>> {}

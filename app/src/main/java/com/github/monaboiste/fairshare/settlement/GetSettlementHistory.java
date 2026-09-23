package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import java.util.List;

public record GetSettlementHistory(SettlementId id)
        implements SettlementQuery<List<EventEnvelope<SettlementId, SettlementEvent>>> {}

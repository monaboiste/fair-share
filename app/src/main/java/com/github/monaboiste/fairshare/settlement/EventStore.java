package com.github.monaboiste.fairshare.settlement;

import java.util.List;

public interface EventStore {
    List<EventEnvelope> load(SettlementId id);

    AppendResult append(SettlementId id, long expectedVersion, List<EventEnvelope> events);
}

package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class CommittedSettlementEvents {
    private final List<Consumer<EventEnvelope<SettlementId, SettlementEvent>>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<EventEnvelope<SettlementId, SettlementEvent>> listener) {
        listeners.add(listener);
    }

    public void publish(List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        for (EventEnvelope<SettlementId, SettlementEvent> event : events) {
            for (var listener : listeners) {
                listener.accept(event);
            }
        }
    }
}

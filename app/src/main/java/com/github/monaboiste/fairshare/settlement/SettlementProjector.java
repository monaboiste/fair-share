package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.common.events.MissingStreamException;
import java.util.HashMap;
import java.util.Map;

public final class SettlementProjector {
    private final Map<SettlementId, SettlementView> views = new HashMap<>();

    public synchronized void rebuild(EventStore<SettlementId, SettlementEvent> store) {
        Map<SettlementId, SettlementView> rebuilt = new HashMap<>();
        for (var stream : store.streams().entrySet()) {
            SettlementProjector projection = new SettlementProjector();
            for (var event : stream.getValue()) {
                projection.accept(event);
            }
            rebuilt.put(stream.getKey(), projection.get(stream.getKey()));
        }
        views.clear();
        views.putAll(rebuilt);
    }

    public synchronized void accept(EventEnvelope<SettlementId, SettlementEvent> event) {
        SettlementView previous = views.get(event.streamId());
        long version = previous == null ? 0 : previous.version();
        if (event.sequence() <= version) {
            return;
        }
        if (event.sequence() != version + 1) {
            throw new IllegalStateException("Gap in Settlement " + event.streamId() + " at " + event.sequence());
        }
        SettlementView next =
                switch (event.payload()) {
                    case SettlementOpened opened -> {
                        if (previous != null) {
                            throw new IllegalStateException("Settlement already projected");
                        }
                        yield new SettlementView(event.streamId(), opened.name(), opened.currency(), event.sequence());
                    }
                    case SettlementRenamed renamed -> {
                        if (previous == null) {
                            throw new IllegalStateException("Settlement opening missing");
                        }
                        yield new SettlementView(
                                event.streamId(), renamed.name(), previous.currency(), event.sequence());
                    }
                };
        views.put(event.streamId(), next);
    }

    public synchronized SettlementView get(SettlementId id) {
        SettlementView view = views.get(id);
        if (view == null) {
            throw new MissingStreamException(id);
        }
        return view;
    }
}

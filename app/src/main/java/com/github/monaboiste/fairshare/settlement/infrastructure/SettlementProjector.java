package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import com.github.monaboiste.fairshare.common.events.EventStore;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView;
import com.github.monaboiste.fairshare.settlement.application.query.SettlementViews;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SettlementProjector implements SettlementViews {
    private final Map<SettlementId, SettlementView> views = new HashMap<>();

    public synchronized void rebuild(EventStore<SettlementId, SettlementEvent> store) {
        Map<SettlementId, SettlementView> rebuilt = new HashMap<>();
        for (var stream : store.streams().entrySet()) {
            SettlementProjector projection = new SettlementProjector();
            projection.accept(stream.getValue());
            projection.findById(stream.getKey()).ifPresent(view -> rebuilt.put(stream.getKey(), view));
        }
        views.clear();
        views.putAll(rebuilt);
    }

    public synchronized void accept(List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        events.forEach(this::accept);
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

    @Override
    public synchronized Optional<SettlementView> findById(SettlementId id) {
        return Optional.ofNullable(views.get(id));
    }
}

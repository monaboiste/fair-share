package com.github.monaboiste.fairshare.settlement.infrastructure;

import com.github.monaboiste.fairshare.common.events.AllEventsReader;
import com.github.monaboiste.fairshare.common.events.CommittedEventsListener;
import com.github.monaboiste.fairshare.common.events.EventEnvelope;
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
import javax.money.Monetary;

public final class SettlementProjector
        implements SettlementViews, CommittedEventsListener<SettlementId, SettlementEvent> {
    private Map<SettlementId, SettlementView> views = new HashMap<>();

    public synchronized void rebuild(AllEventsReader<SettlementId, SettlementEvent> events) {
        views = project(new HashMap<>(), events.readAll(0));
    }

    @Override
    public synchronized void accept(List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        views = project(new HashMap<>(views), events);
    }

    private Map<SettlementId, SettlementView> project(
            Map<SettlementId, SettlementView> updated, List<EventEnvelope<SettlementId, SettlementEvent>> events) {
        for (EventEnvelope<SettlementId, SettlementEvent> event : events) {
            SettlementView previous = updated.get(event.streamId());
            long version = previous == null ? 0 : previous.version();
            if (event.sequence() <= version) {
                continue;
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
                            yield new SettlementView(
                                    event.streamId(),
                                    opened.name(),
                                    Monetary.getCurrency(opened.currencyCode()),
                                    event.sequence());
                        }
                        case SettlementRenamed renamed -> {
                            if (previous == null) {
                                throw new IllegalStateException("Settlement opening missing");
                            }
                            yield new SettlementView(
                                    event.streamId(), renamed.name(), previous.currency(), event.sequence());
                        }
                    };
            updated.put(event.streamId(), next);
        }
        return updated;
    }

    @Override
    public synchronized Optional<SettlementView> findById(SettlementId id) {
        return Optional.ofNullable(views.get(id));
    }
}

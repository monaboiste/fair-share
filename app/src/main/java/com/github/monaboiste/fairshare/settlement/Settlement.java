package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.EventEnvelope;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public final class Settlement {
    private final SettlementId id;
    private final List<EventEnvelope<SettlementId, SettlementEvent>> incoming = new ArrayList<>();
    private long loadedVersion;
    private @Nullable SettlementOpened opening;
    private @Nullable String name;

    public Settlement(SettlementId id) {
        this.id = id;
    }

    public static Settlement replay(SettlementId id, List<EventEnvelope<SettlementId, SettlementEvent>> history) {
        Settlement settlement = new Settlement(id);
        for (EventEnvelope<SettlementId, SettlementEvent> event : history) {
            if (!event.streamId().equals(id) || event.sequence() != settlement.loadedVersion + 1) {
                throw new IllegalArgumentException("Invalid Settlement stream sequence");
            }
            settlement.apply(event.payload());
            settlement.loadedVersion++;
        }
        return settlement;
    }

    public boolean open(String name, CurrencyUnit currency, Clock clock, Supplier<UUID> eventIds) {
        if (opening != null) {
            return opening.equals(new SettlementOpened(name, currency));
        }
        decide(new SettlementOpened(name, currency), clock, eventIds);
        return true;
    }

    public void rename(String name, Clock clock, Supplier<UUID> eventIds) {
        if (opening == null) {
            throw new IllegalStateException("Settlement not opened");
        }
        if (!Objects.equals(this.name, name)) {
            decide(new SettlementRenamed(name), clock, eventIds);
        }
    }

    private void decide(SettlementEvent event, Clock clock, Supplier<UUID> eventIds) {
        incoming.add(
                new EventEnvelope<>(eventIds.get(), id, loadedVersion + incoming.size() + 1, clock.instant(), event));
        apply(event);
    }

    private void apply(SettlementEvent event) {
        switch (event) {
            case SettlementOpened opened -> {
                if (opening != null) {
                    throw new IllegalArgumentException("Settlement already opened");
                }
                opening = opened;
                name = opened.name();
            }
            case SettlementRenamed renamed -> {
                if (opening == null) {
                    throw new IllegalArgumentException("Settlement not opened");
                }
                name = renamed.name();
            }
        }
    }

    public SettlementId id() {
        return id;
    }

    public long loadedVersion() {
        return loadedVersion;
    }

    public List<EventEnvelope<SettlementId, SettlementEvent>> incoming() {
        return List.copyOf(incoming);
    }

    public void committed(long version) {
        loadedVersion = version;
        incoming.clear();
    }
}

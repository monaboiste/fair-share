package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventId;
import java.time.Instant;
import javax.money.CurrencyUnit;

public record SettlementOpened(EventId eventId, String name, CurrencyUnit currency, Instant occurredAt)
        implements SettlementEvent {
    public SettlementOpened(String name, CurrencyUnit currency, Instant occurredAt) {
        this(EventId.random(), name, currency, occurredAt);
    }

    @Override
    public String type() {
        return "SettlementOpened";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

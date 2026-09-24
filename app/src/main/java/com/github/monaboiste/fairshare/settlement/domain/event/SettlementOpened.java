package com.github.monaboiste.fairshare.settlement.domain.event;

import java.time.Instant;
import javax.money.CurrencyUnit;

public record SettlementOpened(String name, CurrencyUnit currency, Instant occurredAt) implements SettlementEvent {
    public SettlementOpened {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
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

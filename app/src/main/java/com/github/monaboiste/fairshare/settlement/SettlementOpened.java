package com.github.monaboiste.fairshare.settlement;

import javax.money.CurrencyUnit;

public record SettlementOpened(String name, CurrencyUnit currency) implements SettlementEvent {
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

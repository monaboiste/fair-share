package com.github.monaboiste.fairshare.settlement.domain.event;

import javax.money.CurrencyUnit;

public record SettlementOpened(String name, CurrencyUnit currency) implements SettlementEvent {
    @Override
    public String type() {
        return "SettlementOpened";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

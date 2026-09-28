package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.valuation.ExchangeRateVersion;

public record ExchangeRateConfigured(ExchangeRateVersion version) implements SettlementEvent {
    @Override
    public String type() {
        return "ExchangeRateConfigured";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

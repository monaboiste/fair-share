package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.pricing.component.SimpleComponentVersion;
import com.github.monaboiste.fairshare.valuation.ExchangeRate;

public record ExchangeRateConfigured(ExchangeRate exchangeRate, SimpleComponentVersion version)
        implements SettlementEvent {
    @Override
    public String type() {
        return "ExchangeRateConfigured";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

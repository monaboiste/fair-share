package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.pricing.component.Validity;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.valuation.ExchangeRate;

public record ConfigureExchangeRate(SettlementId settlementId, ExchangeRate exchangeRate, Validity validity)
        implements SettlementCommand {}

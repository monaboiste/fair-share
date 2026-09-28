package com.github.monaboiste.fairshare.settlement.domain;

import javax.money.CurrencyUnit;

public record ExchangeRateTargetMismatch(SettlementId settlementId, CurrencyUnit targetCurrency)
        implements SettlementRejection {}

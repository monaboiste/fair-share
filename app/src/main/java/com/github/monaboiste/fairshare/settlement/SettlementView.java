package com.github.monaboiste.fairshare.settlement;

import javax.money.CurrencyUnit;

public record SettlementView(SettlementId id, String name, CurrencyUnit currency, long version) {}

package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import javax.money.CurrencyUnit;

public record SettlementView(SettlementId id, String name, CurrencyUnit currency, long version) {}

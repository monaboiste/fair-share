package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import javax.money.CurrencyUnit;

public record OpenSettlement(SettlementId id, SettlementName name, CurrencyUnit currency)
        implements SettlementCommand {}

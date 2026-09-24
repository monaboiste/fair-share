package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import javax.money.CurrencyUnit;

public record OpenSettlement(SettlementId id, String name, CurrencyUnit currency)
        implements SettlementCommand<Result<IdentifierConflict, SettlementId>> {
    public OpenSettlement {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

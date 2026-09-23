package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.Result;
import javax.money.CurrencyUnit;

public record OpenSettlement(SettlementId id, String name, CurrencyUnit currency)
        implements SettlementCommand<Result<IdentifierConflict, SettlementId>> {
    public OpenSettlement {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

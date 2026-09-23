package com.github.monaboiste.fairshare.settlement;

import javax.money.CurrencyUnit;

public record OpenSettlement(SettlementId id, String name, CurrencyUnit currency) implements SettlementCommand {
    public OpenSettlement {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

package com.github.monaboiste.fairshare.settlement;

import java.util.Objects;
import javax.money.CurrencyUnit;

public record SettlementOpened(String name, CurrencyUnit currency) implements SettlementEvent {
    public SettlementOpened {
        if (Objects.requireNonNull(name).isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
        Objects.requireNonNull(currency);
    }
}

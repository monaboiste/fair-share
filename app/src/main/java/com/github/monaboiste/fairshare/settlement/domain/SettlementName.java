package com.github.monaboiste.fairshare.settlement.domain;

public record SettlementName(String value) {
    public SettlementName {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

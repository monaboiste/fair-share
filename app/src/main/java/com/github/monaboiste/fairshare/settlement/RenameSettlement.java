package com.github.monaboiste.fairshare.settlement;

public record RenameSettlement(SettlementId id, String name) implements SettlementCommand {
    public RenameSettlement {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name must not be blank");
        }
    }
}

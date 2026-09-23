package com.github.monaboiste.fairshare.settlement;

import java.util.Objects;
import java.util.UUID;

public record SettlementId(UUID value) {
    public SettlementId {
        Objects.requireNonNull(value);
    }
}

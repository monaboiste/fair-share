package com.github.monaboiste.fairshare.settlement;

import java.util.function.Supplier;

public interface SourcedSettlementRepository {
    <R> R withLock(SettlementId id, Supplier<R> decision);

    Settlement load(SettlementId id);

    Settlement loadOrCreate(SettlementId id);

    void save(Settlement settlement);
}

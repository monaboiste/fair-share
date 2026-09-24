package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.common.domain.ReadRepository;
import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;

public interface SettlementRepository extends ReadRepository<SettlementId, Settlement> {
    CommitResult<SettlementId, SettlementEvent> save(Settlement settlement);
}

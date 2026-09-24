package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.common.domain.ReadRepository;
import com.github.monaboiste.fairshare.common.domain.WriteRepository;

public interface SettlementRepository extends ReadRepository<SettlementId, Settlement>, WriteRepository<Settlement> {}

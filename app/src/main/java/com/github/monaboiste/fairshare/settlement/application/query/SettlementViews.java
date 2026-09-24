package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.common.domain.ReadRepository;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public interface SettlementViews extends ReadRepository<SettlementId, SettlementView> {}

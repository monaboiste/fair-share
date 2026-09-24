package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.common.queries.Query;
import com.github.monaboiste.fairshare.settlement.domain.SettlementRejection;

public sealed interface SettlementQuery<S> extends Query<SettlementRejection, S>
        permits GetSettlement, GetSettlementHistory {}

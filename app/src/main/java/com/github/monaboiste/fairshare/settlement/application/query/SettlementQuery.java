package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.common.queries.Query;

public sealed interface SettlementQuery<R> extends Query<R> permits GetSettlement, GetSettlementHistory {}

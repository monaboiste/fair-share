package com.github.monaboiste.fairshare.settlement.application.query;

public sealed interface SettlementQuery<R> permits GetSettlement, GetSettlementHistory {}

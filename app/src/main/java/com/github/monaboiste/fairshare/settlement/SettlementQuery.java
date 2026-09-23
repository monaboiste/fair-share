package com.github.monaboiste.fairshare.settlement;

public sealed interface SettlementQuery<R> permits GetSettlement, GetSettlementHistory {}

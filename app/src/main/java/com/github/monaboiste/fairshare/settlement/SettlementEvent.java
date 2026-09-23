package com.github.monaboiste.fairshare.settlement;

public sealed interface SettlementEvent permits SettlementOpened, SettlementRenamed {}

package com.github.monaboiste.fairshare.settlement;

import com.github.monaboiste.fairshare.common.events.Event;

public sealed interface SettlementEvent extends Event permits SettlementOpened, SettlementRenamed {}

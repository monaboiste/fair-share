package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.Event;

public sealed interface SettlementEvent extends Event permits SettlementOpened, SettlementRenamed {}

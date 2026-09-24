package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventType;
import java.time.Instant;

@EventType(name = "SettlementOpened", version = 1)
public record SettlementOpened(String name, String currencyCode, Instant occurredAt) implements SettlementEvent {}

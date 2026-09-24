package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventType;
import java.time.Instant;

@EventType(name = "SettlementRenamed", version = 1)
public record SettlementRenamed(String name, Instant occurredAt) implements SettlementEvent {}

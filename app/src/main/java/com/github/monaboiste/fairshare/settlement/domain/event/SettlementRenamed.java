package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.common.events.EventType;
import java.time.Instant;

@EventType(name = "SettlementRenamed", version = 1)
public record SettlementRenamed(EventId eventId, String name, Instant occurredAt) implements SettlementEvent {}

package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventType;
import java.time.Instant;
import javax.money.CurrencyUnit;

@EventType(name = "SettlementOpened", version = 1)
public record SettlementOpened(String name, CurrencyUnit currency, Instant occurredAt) implements SettlementEvent {}

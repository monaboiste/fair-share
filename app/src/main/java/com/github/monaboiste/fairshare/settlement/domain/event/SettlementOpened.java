package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.common.events.EventId;
import com.github.monaboiste.fairshare.common.events.EventType;
import java.time.Instant;
import javax.money.CurrencyUnit;

@EventType(name = "SettlementOpened", version = 1)
public record SettlementOpened(EventId eventId, String name, CurrencyUnit currency, Instant occurredAt)
        implements SettlementEvent {}

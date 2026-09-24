package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.common.domain.AggregateRoot;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import java.time.Instant;
import java.util.List;
import javax.money.CurrencyUnit;
import org.jspecify.annotations.Nullable;

public final class Settlement extends AggregateRoot<SettlementId, SettlementEvent> {
    private final SettlementId id;
    private @Nullable SettlementOpened opening;
    private @Nullable String name;

    private Settlement(SettlementId id) {
        this.id = id;
    }

    public static Settlement open(SettlementId id, String name, CurrencyUnit currency, Instant now) {
        Settlement settlement = new Settlement(id);
        settlement.register(new SettlementOpened(name, currency, now));
        return settlement;
    }

    public static Settlement recreate(SettlementId id, List<SettlementEvent> history) {
        Settlement settlement = new Settlement(id);
        settlement.replay(history);
        return settlement;
    }

    public boolean isOpenedWith(String name, CurrencyUnit currency) {
        return opening != null
                && opening.name().equals(name)
                && opening.currency().equals(currency);
    }

    public void rename(String name, Instant now) {
        if (!name.equals(this.name)) {
            register(new SettlementRenamed(name, now));
        }
    }

    @Override
    public SettlementId id() {
        return id;
    }

    @Override
    protected void apply(SettlementEvent event) {
        switch (event) {
            case SettlementOpened opened -> {
                opening = opened;
                name = opened.name();
            }
            case SettlementRenamed renamed -> name = renamed.name();
        }
    }
}

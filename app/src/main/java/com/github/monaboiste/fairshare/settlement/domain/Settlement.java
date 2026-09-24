package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.common.Result;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateFactory;
import com.github.monaboiste.fairshare.common.eventsourcing.AggregateRoot;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed;
import java.time.Instant;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import org.jspecify.annotations.Nullable;

public final class Settlement extends AggregateRoot<SettlementId, SettlementEvent> {
    private final SettlementId id;
    private @Nullable SettlementName openingName;
    private @Nullable SettlementName name;
    private @Nullable CurrencyUnit currency;

    private Settlement(SettlementId id) {
        this.id = id;
    }

    public static AggregateFactory<SettlementId, Settlement> factory() {
        return Settlement::new;
    }

    public static Settlement open(SettlementId id, SettlementName name, CurrencyUnit currency, Instant now) {
        Settlement settlement = new Settlement(id);
        settlement.register(new SettlementOpened(name.value(), currency.getCurrencyCode(), now));
        return settlement;
    }

    public Result<IdentifierConflict, Settlement> acceptOpeningRetry(SettlementName name, CurrencyUnit currency) {
        return name.equals(openingName) && currency.equals(this.currency)
                ? Result.success(this)
                : Result.failure(new IdentifierConflict(id));
    }

    public void rename(SettlementName name, Instant now) {
        if (!name.equals(this.name)) {
            register(new SettlementRenamed(name.value(), now));
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
                if (openingName != null) {
                    throw new IllegalStateException("Settlement already opened");
                }
                openingName = new SettlementName(opened.name());
                name = openingName;
                currency = Monetary.getCurrency(opened.currencyCode());
            }
            case SettlementRenamed renamed -> {
                if (openingName == null) {
                    throw new IllegalStateException("Settlement opening missing");
                }
                name = new SettlementName(renamed.name());
            }
        }
    }
}

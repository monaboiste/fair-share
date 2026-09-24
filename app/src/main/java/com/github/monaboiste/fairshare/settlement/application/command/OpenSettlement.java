package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.common.events.CommitResult;
import com.github.monaboiste.fairshare.settlement.domain.IdentifierConflict;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementName;
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent;
import javax.money.CurrencyUnit;

public record OpenSettlement(SettlementId id, SettlementName name, CurrencyUnit currency)
        implements SettlementCommand<IdentifierConflict, CommitResult<SettlementId, SettlementEvent>> {}

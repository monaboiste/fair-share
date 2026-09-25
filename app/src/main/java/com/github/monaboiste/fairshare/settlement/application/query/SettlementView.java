package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import java.util.List;
import javax.money.CurrencyUnit;

public record SettlementView(
        SettlementId id, String name, CurrencyUnit currency, long version, List<ParticipantView> participants) {}

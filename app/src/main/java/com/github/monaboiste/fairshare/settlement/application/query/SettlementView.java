package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.netting.Obligation;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import java.util.List;
import java.util.Map;
import javax.money.CurrencyUnit;

public record SettlementView(
        SettlementId id,
        String name,
        CurrencyUnit currency,
        long version,
        List<ParticipantView> participants,
        List<ExpenseView> expenses,
        List<Obligation<ParticipantId>> obligations,
        Map<ParticipantId, Money> balances) {}

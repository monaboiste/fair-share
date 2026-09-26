package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.netting.Obligation;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import javax.money.CurrencyUnit;

public record SettlementView(
        SettlementId id,
        String name,
        CurrencyUnit currency,
        long version,
        List<ParticipantView> participants,
        List<ExpenseView> expenses,
        List<Obligation<ParticipantId>> obligations,
        SequencedMap<ParticipantId, Money> balances) {
    public SettlementView {
        participants = List.copyOf(participants);
        expenses = List.copyOf(expenses);
        obligations = List.copyOf(obligations);
        balances = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(balances));
    }
}

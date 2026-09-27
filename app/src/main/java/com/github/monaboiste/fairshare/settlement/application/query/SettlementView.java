package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.netting.Obligation;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.event.ExchangeRateConfigured;
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
        SequencedMap<ParticipantId, Money> balances,
        List<ExchangeRateConfigured> exchangeRates) {
    public SettlementView(
            SettlementId id,
            String name,
            CurrencyUnit currency,
            long version,
            List<ParticipantView> participants,
            List<ExpenseView> expenses,
            List<Obligation<ParticipantId>> obligations,
            SequencedMap<ParticipantId, Money> balances) {
        this(id, name, currency, version, participants, expenses, obligations, balances, List.of());
    }

    public SettlementView {
        participants = List.copyOf(participants);
        expenses = List.copyOf(expenses);
        obligations = List.copyOf(obligations);
        exchangeRates = List.copyOf(exchangeRates);
        balances = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(balances));
    }
}

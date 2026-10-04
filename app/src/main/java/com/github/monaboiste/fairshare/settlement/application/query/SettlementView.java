package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.netting.Obligation;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementStatus;
import com.github.monaboiste.fairshare.valuation.ExchangeRateVersion;
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
        List<RepaymentView> repayments,
        List<Obligation<ParticipantId>> obligations,
        SequencedMap<ParticipantId, Money> balances,
        List<ExchangeRateVersion> exchangeRates,
        SettlementStatus status) {
    public SettlementView {
        participants = List.copyOf(participants);
        expenses = List.copyOf(expenses);
        repayments = List.copyOf(repayments);
        obligations = List.copyOf(obligations);
        exchangeRates = List.copyOf(exchangeRates);
        balances = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(balances));
    }
}

package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

public sealed interface ShareAllocation permits EqualShareAllocation, ExactShareAllocation, WeightedShareAllocation {
    SequencedSet<ParticipantId> recipients();

    Optional<SettlementRejection> validate(SettlementId settlementId, ExpenseId expenseId, Money originalAmount);

    List<Share> resolve(Money amount);
}

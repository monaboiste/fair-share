package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;

/** Defines how an Expense is divided into Shares. */
public sealed interface ShareAllocation permits EqualShareAllocation, ExactShareAllocation, WeightedShareAllocation {
    /** Returns recipients in presentation order, including those whose resolved Share is zero. */
    SequencedSet<ParticipantId> recipients();

    /** Validates the allocation against the original Expense before Valuation. */
    Optional<SettlementRejection> validate(SettlementId settlementId, ExpenseId expenseId, Money originalAmount);

    /** Resolves Shares from the Valuation, omitting zero Shares. */
    List<Share> resolve(Money amount);
}

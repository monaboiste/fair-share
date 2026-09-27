package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;

public record EqualShareAllocation(SequencedSet<ParticipantId> recipients) implements ShareAllocation {
    public EqualShareAllocation(Collection<ParticipantId> recipients) {
        this(new LinkedHashSet<>(recipients));
    }

    public EqualShareAllocation {
        recipients.forEach(Objects::requireNonNull);
        recipients = Collections.unmodifiableSequencedSet(new LinkedHashSet<>(recipients));
    }

    @Override
    public Optional<SettlementRejection> validate(
            SettlementId settlementId, ExpenseId expenseId, Money originalAmount) {
        return Optional.empty();
    }

    @Override
    public List<Share> resolve(Money amount) {
        SequencedMap<ParticipantId, BigInteger> proportions = new LinkedHashMap<>();
        recipients.forEach(recipient -> proportions.put(recipient, BigInteger.ONE));
        return LargestRemainderApportionment.apportion(proportions, amount);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EqualShareAllocation(SequencedSet<ParticipantId> otherRecipients)
                && List.copyOf(recipients).equals(List.copyOf(otherRecipients));
    }

    @Override
    public int hashCode() {
        return List.copyOf(recipients).hashCode();
    }
}

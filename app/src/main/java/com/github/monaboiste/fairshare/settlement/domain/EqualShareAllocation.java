package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.SequencedSet;

public record EqualShareAllocation(SequencedSet<ParticipantId> recipients) implements ShareAllocation {
    public EqualShareAllocation(Collection<ParticipantId> recipients) {
        this(new LinkedHashSet<>(recipients));
    }

    public EqualShareAllocation {
        recipients = Collections.unmodifiableSequencedSet(new LinkedHashSet<>(recipients));
    }

    @Override
    public List<Share> resolve(Money amount) {
        SequencedMap<ParticipantId, BigInteger> weights = new LinkedHashMap<>();
        recipients.forEach(recipient -> weights.put(recipient, BigInteger.ONE));
        return ShareApportionment.resolve(weights, amount);
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

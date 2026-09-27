package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.SequencedSet;

public record WeightedShareAllocation(SequencedMap<ParticipantId, Integer> weights) implements ShareAllocation {
    public WeightedShareAllocation {
        weights = Collections.unmodifiableSequencedMap(new LinkedHashMap<>(weights));
        weights.forEach((recipient, weight) -> {
            Objects.requireNonNull(recipient);
            Objects.requireNonNull(weight);
        });
    }

    @Override
    public SequencedSet<ParticipantId> recipients() {
        return weights.sequencedKeySet();
    }

    @Override
    public List<Share> resolve(Money amount) {
        SequencedMap<ParticipantId, BigInteger> proportions = new LinkedHashMap<>();
        weights.forEach((recipient, weight) -> proportions.put(recipient, BigInteger.valueOf(weight)));
        return ShareApportionment.resolve(proportions, amount);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WeightedShareAllocation(SequencedMap<ParticipantId, Integer> otherWeights)
                && List.copyOf(weights.entrySet()).equals(List.copyOf(otherWeights.entrySet()));
    }

    @Override
    public int hashCode() {
        return List.copyOf(weights.entrySet()).hashCode();
    }
}

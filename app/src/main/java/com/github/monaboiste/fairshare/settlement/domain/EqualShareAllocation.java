package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        if (recipients.isEmpty()) {
            throw new IllegalStateException("Cannot resolve an empty Share Allocation");
        }
        Money[] division = amount.divideAndRemainder(BigDecimal.valueOf(recipients.size()));
        Money unit = amount.smallestUnit();
        int residual = division[1].value().divideToIntegralValue(unit.value()).intValueExact();
        List<ParticipantId> priority = recipients.stream()
                .sorted(Comparator.comparing(ParticipantId::value))
                .toList();
        Map<ParticipantId, Money> resolved = new HashMap<>();
        for (ParticipantId recipient : recipients) {
            resolved.put(recipient, division[0]);
        }
        for (int i = 0; i < residual; i++) {
            ParticipantId recipient = priority.get(i);
            resolved.put(recipient, resolved.get(recipient).add(unit));
        }
        List<Share> shares = new ArrayList<>();
        for (ParticipantId recipient : recipients) {
            Money share = resolved.get(recipient);
            if (!share.isZero()) {
                shares.add(new Share(recipient, share));
            }
        }
        return List.copyOf(shares);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EqualShareAllocation allocation
                && List.copyOf(recipients).equals(List.copyOf(allocation.recipients));
    }

    @Override
    public int hashCode() {
        return List.copyOf(recipients).hashCode();
    }
}

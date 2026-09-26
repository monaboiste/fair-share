package com.github.monaboiste.fairshare.settlement.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record EqualShareAllocation(Set<ParticipantId> recipients) implements ShareAllocation {
    public EqualShareAllocation(Collection<ParticipantId> recipients) {
        this(new LinkedHashSet<>(recipients));
    }

    public EqualShareAllocation {
        recipients = Collections.unmodifiableSet(new LinkedHashSet<>(recipients));
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

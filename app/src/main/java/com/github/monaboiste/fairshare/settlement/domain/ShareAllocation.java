package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.List;
import java.util.SequencedSet;

public sealed interface ShareAllocation permits EqualShareAllocation {
    SequencedSet<ParticipantId> recipients();

    List<Share> resolve(Money amount);
}

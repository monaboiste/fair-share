package com.github.monaboiste.fairshare.settlement.domain;

import java.util.SequencedSet;

public sealed interface ShareAllocation permits EqualShareAllocation {
    SequencedSet<ParticipantId> recipients();
}

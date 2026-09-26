package com.github.monaboiste.fairshare.settlement.domain;

import java.util.Set;

public sealed interface ShareAllocation permits EqualShareAllocation {
    Set<ParticipantId> recipients();
}

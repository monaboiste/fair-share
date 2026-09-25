package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;

public record ParticipantRemoved(ParticipantId participantId) implements SettlementEvent {
    @Override
    public String type() {
        return "ParticipantRemoved";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;

public record ParticipantAdded(ParticipantId participantId, String name) implements SettlementEvent {
    @Override
    public String type() {
        return "ParticipantAdded";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

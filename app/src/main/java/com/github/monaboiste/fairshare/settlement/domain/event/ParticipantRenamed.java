package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;

public record ParticipantRenamed(ParticipantId participantId, String name) implements SettlementEvent {
    @Override
    public String type() {
        return "ParticipantRenamed";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

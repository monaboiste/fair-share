package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record AddParticipant(SettlementId settlementId, ParticipantId participantId, ParticipantName name)
        implements SettlementCommand {}

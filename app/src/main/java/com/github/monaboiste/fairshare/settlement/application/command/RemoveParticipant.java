package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;

public record RemoveParticipant(SettlementId settlementId, ParticipantId participantId) implements SettlementCommand {}

package com.github.monaboiste.fairshare.settlement.domain;

public record ParticipantReferenced(SettlementId settlementId, ParticipantId participantId)
        implements SettlementRejection {}

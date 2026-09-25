package com.github.monaboiste.fairshare.settlement.domain;

public record ParticipantNotFound(SettlementId settlementId, ParticipantId participantId)
        implements SettlementRejection {}

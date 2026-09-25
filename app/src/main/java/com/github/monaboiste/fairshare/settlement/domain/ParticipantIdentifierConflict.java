package com.github.monaboiste.fairshare.settlement.domain;

public record ParticipantIdentifierConflict(SettlementId settlementId, ParticipantId participantId)
        implements SettlementRejection {}

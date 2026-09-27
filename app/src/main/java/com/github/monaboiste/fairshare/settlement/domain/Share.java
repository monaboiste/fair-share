package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;

/** The part of a valued Expense assigned to one Participant. */
public record Share(ParticipantId participantId, Money amount) {}

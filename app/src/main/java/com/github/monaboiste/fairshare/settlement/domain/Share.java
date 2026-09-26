package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;

public record Share(ParticipantId participantId, Money amount) {}

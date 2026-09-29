package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDate;

public record RepaymentDetails(
        RepaymentId repaymentId, LocalDate paidOn, ParticipantId payer, ParticipantId recipient, Money amount) {}

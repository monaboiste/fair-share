package com.github.monaboiste.fairshare.settlement.application.query;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId;
import java.time.LocalDate;

public record RepaymentView(
        RepaymentId id, LocalDate paidOn, ParticipantId payer, ParticipantId recipient, Money amount, Status status) {
    public enum Status {
        ACTIVE,
        CANCELLED
    }
}

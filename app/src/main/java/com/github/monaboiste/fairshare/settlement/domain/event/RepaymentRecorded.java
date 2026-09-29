package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentDetails;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId;
import java.time.LocalDate;

public record RepaymentRecorded(
        RepaymentId repaymentId, LocalDate paidOn, ParticipantId payer, ParticipantId recipient, Money amount)
        implements SettlementEvent {
    public RepaymentDetails details() {
        return new RepaymentDetails(repaymentId, paidOn, payer, recipient, amount);
    }

    @Override
    public String type() {
        return "RepaymentRecorded";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

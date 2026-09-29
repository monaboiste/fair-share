package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import java.time.LocalDate;

public record RecordRepayment(
        SettlementId settlementId,
        RepaymentId repaymentId,
        LocalDate paidOn,
        ParticipantId payer,
        ParticipantId recipient,
        Money amount)
        implements SettlementCommand {}

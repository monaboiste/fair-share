package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDate;

public record ExpenseInput(
        ExpenseId expenseId,
        ExpenseDescription description,
        LocalDate incurredOn,
        ParticipantId payer,
        Money amount,
        ShareAllocation allocation) {}

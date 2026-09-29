package com.github.monaboiste.fairshare.settlement.application.command;

import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.SettlementId;
import com.github.monaboiste.fairshare.settlement.domain.ShareAllocation;
import com.github.monaboiste.fairshare.valuation.ExchangeRate;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record RecordExpense(
        SettlementId settlementId,
        ExpenseId expenseId,
        ExpenseDescription description,
        LocalDate incurredOn,
        ParticipantId payer,
        Money amount,
        ShareAllocation allocation,
        @Nullable ExchangeRate exchangeRateOverride)
        implements SettlementCommand {
    public RecordExpense(
            SettlementId settlementId,
            ExpenseId expenseId,
            ExpenseDescription description,
            LocalDate incurredOn,
            ParticipantId payer,
            Money amount,
            ShareAllocation allocation) {
        this(settlementId, expenseId, description, incurredOn, payer, amount, allocation, null);
    }
}

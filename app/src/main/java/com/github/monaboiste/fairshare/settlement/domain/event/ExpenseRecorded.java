package com.github.monaboiste.fairshare.settlement.domain.event;

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId;
import com.github.monaboiste.fairshare.quantity.money.Money;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription;
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId;
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId;
import com.github.monaboiste.fairshare.settlement.domain.Share;
import com.github.monaboiste.fairshare.settlement.domain.ShareAllocation;
import com.github.monaboiste.fairshare.valuation.ExchangeRate;
import java.time.LocalDate;
import java.util.List;

public record ExpenseRecorded(
        ExpenseId expenseId,
        ExpenseDescription description,
        LocalDate incurredOn,
        ParticipantId payer,
        Money originalAmount,
        ShareAllocation allocation,
        ComponentVersionId componentVersionId,
        ExchangeRate exchangeRate,
        Money valuation,
        List<Share> shares)
        implements SettlementEvent {
    public ExpenseRecorded {
        shares = List.copyOf(shares);
    }

    @Override
    public String type() {
        return "ExpenseRecorded";
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}

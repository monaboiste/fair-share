package com.github.monaboiste.fairshare.settlement.application.query;

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

public record ExpenseView(
        ExpenseId id,
        ExpenseDescription description,
        LocalDate incurredOn,
        ParticipantId payer,
        Money originalAmount,
        ShareAllocation allocation,
        ComponentVersionId componentVersionId,
        ExchangeRate exchangeRate,
        Money valuation,
        List<Share> shares,
        Status status) {
    public ExpenseView {
        shares = List.copyOf(shares);
    }

    public enum Status {
        ACTIVE
    }
}

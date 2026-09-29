package com.github.monaboiste.fairshare.settlement.domain;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDate;
import java.util.Optional;
import javax.money.CurrencyUnit;

public record RepaymentDetails(
        RepaymentId repaymentId, LocalDate paidOn, ParticipantId payer, ParticipantId recipient, Money amount) {
    public Optional<SettlementRejection> validateAmount(SettlementId settlementId, CurrencyUnit settlementCurrency) {
        if (amount.isZero() || amount.isNegative()) {
            return Optional.of(new NonPositiveRepaymentAmount(settlementId, repaymentId));
        }
        if (!amount.currencyUnit().equals(settlementCurrency)) {
            return Optional.of(new RepaymentCurrencyMismatch(settlementId, repaymentId));
        }
        if (amount.value().stripTrailingZeros().scale() > settlementCurrency.getDefaultFractionDigits()) {
            return Optional.of(new RepaymentAmountPrecisionExceeded(settlementId, repaymentId));
        }
        return Optional.empty();
    }
}

package com.github.monaboiste.fairshare.netting;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.money.CurrencyUnit;

/** Participants and their positive debtor-to-creditor Proposed Repayments in one currency. */
public record ProposedRepaymentGraph<P>(
        Set<P> participants, List<ProposedRepayment<P>> proposedRepayments, CurrencyUnit currency) {

    public ProposedRepaymentGraph {
        Objects.requireNonNull(participants, "Proposed repayment graph participants are required");
        Objects.requireNonNull(proposedRepayments, "Proposed repayment graph repayments are required");
        Objects.requireNonNull(currency, "Proposed repayment graph currency is required");
        participants = Set.copyOf(participants);
        proposedRepayments = List.copyOf(proposedRepayments);
        Set<List<P>> directed = new HashSet<>();
        for (ProposedRepayment<P> repayment : proposedRepayments) {
            if (!participants.contains(repayment.debtor()) || !participants.contains(repayment.creditor())) {
                throw new IllegalArgumentException("Proposed repayment references an unknown participant");
            }
            if (!repayment.amount().currencyUnit().equals(currency)) {
                throw new IllegalArgumentException("All repayments must use " + currency.getCurrencyCode());
            }
            if (!directed.add(List.of(repayment.debtor(), repayment.creditor()))) {
                throw new IllegalArgumentException("Proposed repayments must not contain parallel edges");
            }
            if (directed.contains(List.of(repayment.creditor(), repayment.debtor()))) {
                throw new IllegalArgumentException("Proposed repayments must not contain cycles");
            }
        }
    }

    public static <P> ProposedRepaymentGraph<P> of(
            Set<P> participants, List<ProposedRepayment<P>> proposedRepayments, CurrencyUnit currency) {
        return new ProposedRepaymentGraph<>(participants, proposedRepayments, currency);
    }
}

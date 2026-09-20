package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Greedy netting that repeatedly settles the largest remaining debtor against the largest remaining creditor.
 *
 * <p>Each repayment zeroes at least one participant, so the output holds at most {@code max(0, unbalanced - 1)} edges
 * with no parallel edges or cycles. Ties are broken by the participant order, making the output deterministic. This is
 * the standard minimum-cash-flow heuristic; exact minimization is NP-hard.
 */
final class GreedyNetting implements Netting {

    @Override
    public <P> ProposedRepayments<P> net(
            Obligations<P> obligations, ParticipantComparator<? super P> order, LocalDateTime asOf) {
        Balances<P> balances = Balances.of(obligations, asOf);
        List<ProposedRepayment<P>> repayments = match(balances, order);
        return ProposedRepayments.of(obligations.participants(), repayments, obligations.currency());
    }

    /**
     * Matches debtors with creditors, always taking the largest remaining amounts first.
     *
     * <p>Each iteration fully settles at least one participant. Participants with a remaining balance are returned to
     * their respective queue.
     */
    private static <P> List<ProposedRepayment<P>> match(Balances<P> balances, ParticipantComparator<? super P> order) {
        Comparator<Owed<P>> byOwed = Comparator.<Owed<P>, Money>comparing(Owed::owed, Money::compareTo)
                .reversed()
                .thenComparing(Owed::participant, order);

        PriorityQueue<Owed<P>> debtors = queue(balances.debtors(), byOwed);
        PriorityQueue<Owed<P>> creditors = queue(balances.creditors(), byOwed);

        List<ProposedRepayment<P>> repayments = new ArrayList<>();

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Owed<P> debtor = debtors.remove();
            Owed<P> creditor = creditors.remove();

            Money transfer = Money.min(debtor.owed(), creditor.owed());
            repayments.add(new ProposedRepayment<>(debtor.participant(), creditor.participant(), transfer));

            requeue(debtors, debtor, transfer);
            requeue(creditors, creditor, transfer);
        }

        return repayments;
    }

    private static <P> PriorityQueue<Owed<P>> queue(Map<P, Money> balances, Comparator<Owed<P>> comparator) {
        PriorityQueue<Owed<P>> queue = new PriorityQueue<>(comparator);
        balances.forEach((participant, owed) -> queue.add(new Owed<>(participant, owed)));
        return queue;
    }

    private static <P> void requeue(PriorityQueue<Owed<P>> queue, Owed<P> owed, Money paid) {
        Money remaining = owed.owed().subtract(paid);
        if (!remaining.isZero()) {
            queue.add(new Owed<>(owed.participant(), remaining));
        }
    }

    /** A participant with the positive amount they currently owe or are owed. */
    private record Owed<P>(P participant, Money owed) {}
}

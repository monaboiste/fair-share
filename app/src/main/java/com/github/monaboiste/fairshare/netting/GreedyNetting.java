package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    public <P> ProposedRepaymentGraph<P> net(
            ObligationGraph<P> graph, ParticipantComparator<? super P> order, LocalDateTime asOf) {
        Balances<P> balances = Balances.of(graph, asOf);
        List<ProposedRepayment<P>> repayments = match(balances, order);
        return ProposedRepaymentGraph.of(graph.participants(), repayments, graph.currency());
    }

    private static <P> List<ProposedRepayment<P>> match(Balances<P> balances, ParticipantComparator<? super P> order) {
        Comparator<Owed<P>> byOwed = Comparator.comparing((Owed<P> owed) -> owed.owed(), Money::compareTo)
                .reversed()
                .thenComparing(owed -> owed.participant(), order);

        PriorityQueue<Owed<P>> debtors = new PriorityQueue<>(byOwed);
        balances.debtors().forEach((participant, owed) -> debtors.add(new Owed<>(participant, owed)));
        PriorityQueue<Owed<P>> creditors = new PriorityQueue<>(byOwed);
        balances.creditors().forEach((participant, owed) -> creditors.add(new Owed<>(participant, owed)));

        List<ProposedRepayment<P>> repayments = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Owed<P> debtor = debtors.poll();
            Owed<P> creditor = creditors.poll();

            Money transfer = Money.min(debtor.owed(), creditor.owed());
            repayments.add(new ProposedRepayment<>(debtor.participant(), creditor.participant(), transfer));

            Money remainingDebt = debtor.owed().subtract(transfer);
            Money remainingCredit = creditor.owed().subtract(transfer);
            if (!remainingDebt.isZero()) {
                debtors.add(new Owed<>(debtor.participant(), remainingDebt));
            }
            if (!remainingCredit.isZero()) {
                creditors.add(new Owed<>(creditor.participant(), remainingCredit));
            }
        }
        return repayments;
    }

    private record Owed<P>(P participant, Money owed) {}
}

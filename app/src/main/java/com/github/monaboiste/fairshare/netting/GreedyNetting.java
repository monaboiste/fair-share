package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.graphs.Edge;
import com.github.monaboiste.fairshare.graphs.Graph;
import com.github.monaboiste.fairshare.graphs.Node;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class GreedyNetting implements Netting {

    @Override
    public <P> ProposedRepaymentGraph<P> net(ObligationGraph<P> graph, Comparator<P> participantOrder) {
        Objects.requireNonNull(graph, "Obligation graph is required");
        Objects.requireNonNull(participantOrder, "Participant order is required");

        Graph<P, Money> normalized = aggregateAndCompensate(graph);
        for (Node<P> vertex : normalized.vertices()) {
            if (!graph.participants().contains(vertex.property())) {
                throw new IllegalStateException("Normalized graph references an unknown participant");
            }
        }
        Map<P, Money> balances = balances(graph, normalized);
        List<ProposedRepayment<P>> repayments = match(balances, participantOrder);

        Graph<P, Money> proposal = new Graph<>();
        for (ProposedRepayment<P> repayment : repayments) {
            proposal.addEdge(
                    new Edge<>(new Node<>(repayment.debtor()), new Node<>(repayment.creditor()), repayment.amount()));
        }
        if (proposal.findFirstCycle().isPresent()) {
            throw new IllegalStateException("Proposed repayments must not contain cycles");
        }
        return ProposedRepaymentGraph.of(graph.participants(), repayments, graph.currency());
    }

    private static <P> Graph<P, Money> aggregateAndCompensate(ObligationGraph<P> graph) {
        Map<P, Map<P, Money>> directed = new HashMap<>();
        for (Obligation<P> obligation : graph.obligations()) {
            if (obligation.from().equals(obligation.to()) || obligation.amount().isZero()) {
                continue;
            }
            directed.computeIfAbsent(obligation.from(), _ -> new HashMap<>())
                    .merge(obligation.to(), obligation.amount(), Money::add);
        }
        Money zero = Money.zero(graph.currency().getCurrencyCode());
        Set<Set<P>> compensated = new HashSet<>();
        Graph<P, Money> normalized = new Graph<>();
        for (Map.Entry<P, Map<P, Money>> entry : directed.entrySet()) {
            for (Map.Entry<P, Money> inner : entry.getValue().entrySet()) {
                P from = entry.getKey();
                P to = inner.getKey();
                if (from.equals(to) || !compensated.add(Set.of(from, to))) {
                    continue;
                }
                Money forward = directed.getOrDefault(from, Map.of()).getOrDefault(to, zero);
                Money backward = directed.getOrDefault(to, Map.of()).getOrDefault(from, zero);
                Money net = forward.subtract(backward);
                if (net.isZero()) {
                    continue;
                }
                if (net.isNegative()) {
                    normalized.addEdge(new Edge<>(new Node<>(to), new Node<>(from), net.negate()));
                } else {
                    normalized.addEdge(new Edge<>(new Node<>(from), new Node<>(to), net));
                }
            }
        }
        return normalized;
    }

    private static <P> Map<P, Money> balances(ObligationGraph<P> graph, Graph<P, Money> normalized) {
        Money zero = Money.zero(graph.currency().getCurrencyCode());
        Map<P, Money> balances = new HashMap<>();
        for (P participant : graph.participants()) {
            balances.put(participant, zero);
        }
        for (Edge<P, Money> edge : normalized.edges()) {
            balances.merge(edge.from().property(), edge.property().negate(), Money::add);
            balances.merge(edge.to().property(), edge.property(), Money::add);
        }
        return balances;
    }

    private static <P> List<ProposedRepayment<P>> match(Map<P, Money> balances, Comparator<P> participantOrder) {
        Comparator<Balance<P>> byOwed = Comparator.comparing(Balance<P>::owed, Money::compareTo)
                .reversed()
                .thenComparing(Balance::participant, participantOrder);
        Deque<Balance<P>> debtors = new ArrayDeque<>(balances.entrySet().stream()
                .filter(entry -> entry.getValue().isNegative())
                .map(entry -> new Balance<>(entry.getKey(), entry.getValue().abs()))
                .sorted(byOwed)
                .toList());
        Deque<Balance<P>> creditors = new ArrayDeque<>(balances.entrySet().stream()
                .filter(entry -> !entry.getValue().isZero() && !entry.getValue().isNegative())
                .map(entry -> new Balance<>(entry.getKey(), entry.getValue()))
                .sorted(byOwed)
                .toList());
        List<ProposedRepayment<P>> repayments = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Balance<P> debtor = debtors.peekFirst();
            Balance<P> creditor = creditors.peekFirst();
            Money transfer = Money.min(debtor.owed(), creditor.owed());
            repayments.add(new ProposedRepayment<>(debtor.participant(), creditor.participant(), transfer));
            Balance<P> leftDebtor =
                    new Balance<>(debtor.participant(), debtor.owed().subtract(transfer));
            Balance<P> leftCreditor =
                    new Balance<>(creditor.participant(), creditor.owed().subtract(transfer));
            debtors.removeFirst();
            creditors.removeFirst();
            if (!leftDebtor.owed().isZero()) {
                insertSorted(debtors, leftDebtor, byOwed);
            }
            if (!leftCreditor.owed().isZero()) {
                insertSorted(creditors, leftCreditor, byOwed);
            }
        }
        return repayments;
    }

    private static <P> void insertSorted(Deque<Balance<P>> queue, Balance<P> balance, Comparator<Balance<P>> byOwed) {
        List<Balance<P>> sorted = new ArrayList<>(queue);
        int position = 0;
        while (position < sorted.size() && byOwed.compare(sorted.get(position), balance) <= 0) {
            position++;
        }
        sorted.add(position, balance);
        queue.clear();
        queue.addAll(sorted);
    }

    private record Balance<P>(P participant, Money owed) {}
}

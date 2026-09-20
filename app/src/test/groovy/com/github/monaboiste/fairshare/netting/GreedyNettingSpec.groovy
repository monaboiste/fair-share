package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class GreedyNettingSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")

    private final Netting netting = Netting.greedy()

    def "matches a simple debtor with a creditor"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob"],
                [["ada", "bob", 10]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))]
    }

    def "settles two debtors with one creditor in two transfers"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob", "cid", "dan"],
                [["ada", "cid", 10], ["bob", "cid", 10], ["cid", "dan", 20]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [
            new ProposedRepayment<>("ada", "dan", Money.of(10, "PLN")),
            new ProposedRepayment<>("bob", "dan", Money.of(10, "PLN"))]
    }

    def "eliminates an original obligation cycle into no repayments"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob", "cid"],
                [["ada", "bob", 10], ["bob", "cid", 10], ["cid", "ada", 10]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments().isEmpty()
    }

    def "aggregates parallel obligations and compensates opposite ones"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob"],
                [["ada", "bob", 10], ["ada", "bob", 5], ["bob", "ada", 7]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>("ada", "bob", Money.of(8, "PLN"))]
    }

    def "removes loops and zero obligations before netting"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob"],
                [["ada", "ada", 10], ["ada", "bob", 0], ["ada", "bob", 10]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))]
    }

    def "returns no repayments for settled input"() {
        given:
        ObligationGraph<String> graph = ObligationGraph.of(["ada", "bob"] as Set, List.of(), PLN)

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments().isEmpty()
        proposal.participants() == ["ada", "bob"] as Set
    }

    def "orders equal balances deterministically by participant order"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob", "cid", "dan"],
                [["ada", "cid", 10], ["bob", "dan", 10]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [
            new ProposedRepayment<>("ada", "cid", Money.of(10, "PLN")),
            new ProposedRepayment<>("bob", "dan", Money.of(10, "PLN"))]
    }

    def "preserves every participant balance"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob", "cid", "dan"],
                [["ada", "bob", 30], ["bob", "cid", 10], ["cid", "dan", 5], ["dan", "ada", 5]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        balancesOf(graph) == balancesOfProposal(proposal)
    }

    def "always matches the largest remaining debtor with the largest remaining creditor"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob", "cid", "dan"],
                [["bob", "cid", 90], ["ada", "cid", 5], ["ada", "dan", 95]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [
            new ProposedRepayment<>("ada", "cid", Money.of(95, "PLN")),
            new ProposedRepayment<>("bob", "dan", Money.of(90, "PLN")),
            new ProposedRepayment<>("ada", "dan", Money.of(5, "PLN"))]
    }

    def "produces a valid graph within the edge bound"() {
        given:
        ObligationGraph<String> graph = obligationGraph(
                ["ada", "bob", "cid", "dan", "eva"],
                [["ada", "bob", 30], ["bob", "cid", 10], ["cid", "dan", 5], ["dan", "eva", 5],
                 ["eva", "ada", 5], ["ada", "cid", 7]])

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        hasNoLoops(proposal)
        hasNoZeroEdges(proposal)
        hasNoParallelEdges(proposal)
        hasNoCycles(proposal)
        proposal.proposedRepayments().size() <= unbalancedParticipants(graph) - 1
    }

    def "equal inputs produce equal output ordering"() {
        given:
        ObligationGraph<String> first = obligationGraph(
                ["ada", "bob", "cid", "dan"],
                [["ada", "cid", 10], ["bob", "dan", 10]])
        ObligationGraph<String> second = obligationGraph(
                ["ada", "bob", "cid", "dan"],
                [["bob", "dan", 10], ["ada", "cid", 10]])

        when:
        ProposedRepaymentGraph<String> firstProposal = netting.net(first, order())
        ProposedRepaymentGraph<String> secondProposal = netting.net(second, order())

        then:
        firstProposal.proposedRepayments() == secondProposal.proposedRepayments()
    }

    def "respects the smallest unit of #currencyCode"() {
        given:
        CurrencyUnit currency = Monetary.getCurrency(currencyCode)
        ObligationGraph<String> graph = ObligationGraph.of(
                ["ada", "bob", "cid"] as Set,
                [Obligation.of("ada", "bob", Money.of(new BigDecimal(owed), currencyCode)),
                 Obligation.of("bob", "cid", Money.of(new BigDecimal(owed), currencyCode))],
                currency)

        when:
        ProposedRepaymentGraph<String> proposal = netting.net(graph, order())

        then:
        proposal.proposedRepayments() == [
            new ProposedRepayment<>("ada", "cid", Money.of(new BigDecimal(owed), currencyCode))]

        where:
        currencyCode | owed
        "JPY"        | "100"
        "USD"        | "10.01"
        "KWD"        | "10.001"
    }

    def "supports generic participants without leaking graph types"() {
        given:
        ObligationGraph<Integer> graph = ObligationGraph.of(
                [3, 1, 2] as Set,
                [Obligation.of(3, 1, Money.of(10, "PLN"))],
                PLN)

        when:
        ProposedRepaymentGraph<Integer> proposal =
                netting.net(graph, { a, b -> a <=> b } as ParticipantComparator<Integer>)

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>(3, 1, Money.of(10, "PLN"))]
    }

    def "rejects a null obligation graph"() {
        when:
        netting.net(null, order())

        then:
        thrown(NullPointerException)
    }

    def "rejects a null participant order"() {
        given:
        ObligationGraph<String> graph = obligationGraph(["ada", "bob"], [["ada", "bob", 10]])

        when:
        netting.net(graph, null)

        then:
        thrown(NullPointerException)
    }

    private static ParticipantComparator<String> order() {
        { a, b -> a <=> b } as ParticipantComparator<String>
    }

    private static ObligationGraph<String> obligationGraph(List<String> participants, List<List> entries) {
        List<Obligation<String>> obligations = entries.collect { entry ->
            Obligation.of(entry[0] as String, entry[1] as String, Money.of(entry[2], "PLN"))
        }
        return ObligationGraph.of(participants as Set, obligations, PLN)
    }

    private static Map<String, Money> balancesOf(ObligationGraph<String> graph) {
        Map<String, Money> balances = [:].withDefault { Money.zero("PLN") }
        graph.participants().each { balances[it] = Money.zero("PLN") }
        graph.obligations().each { obligation ->
            if (obligation.from() == obligation.to() || obligation.amount().isZero()) {
                return
            }
            balances[obligation.from()] = balances[obligation.from()].subtract(obligation.amount())
            balances[obligation.to()] = balances[obligation.to()].add(obligation.amount())
        }
        return balances
    }

    private static Map<String, Money> balancesOfProposal(ProposedRepaymentGraph<String> proposal) {
        Map<String, Money> balances = [:].withDefault { Money.zero("PLN") }
        proposal.participants().each { balances[it] = Money.zero("PLN") }
        proposal.proposedRepayments().each { repayment ->
            balances[repayment.debtor()] = balances[repayment.debtor()].subtract(repayment.amount())
            balances[repayment.creditor()] = balances[repayment.creditor()].add(repayment.amount())
        }
        return balances
    }

    private static boolean hasNoLoops(ProposedRepaymentGraph<String> proposal) {
        !proposal.proposedRepayments().any { it.debtor() == it.creditor() }
    }

    private static boolean hasNoZeroEdges(ProposedRepaymentGraph<String> proposal) {
        !proposal.proposedRepayments().any { it.amount().isZero() || it.amount().isNegative() }
    }

    private static boolean hasNoParallelEdges(ProposedRepaymentGraph<String> proposal) {
        List<Set<String>> pairs = proposal.proposedRepayments().collect { [it.debtor(), it.creditor()] as Set }
        pairs.size() == pairs.toSet().size()
    }

    private static boolean hasNoCycles(ProposedRepaymentGraph<String> proposal) {
        Map<String, Set<String>> outgoing = [:].withDefault { [] as Set }
        proposal.proposedRepayments().each { outgoing[it.debtor()] = outgoing[it.debtor()] + it.creditor() }
        proposal.participants().every { !reaches(it, it, outgoing) }
    }

    private static boolean reaches(String start, String current, Map<String, Set<String>> outgoing) {
        for (String next : outgoing.getOrDefault(current, [] as Set)) {
            if (next == start || reaches(start, next, outgoing)) {
                return true
            }
        }
        return false
    }

    private static int unbalancedParticipants(ObligationGraph<String> graph) {
        balancesOf(graph).values().count { !it.isZero() }
    }
}

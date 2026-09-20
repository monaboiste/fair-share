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
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob"],
                [["ada", "bob", 10]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))]
    }

    def "settles two debtors with one creditor in two transfers"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob", "cid", "dan"],
                [["ada", "cid", 10], ["bob", "cid", 10], ["cid", "dan", 20]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments() as Set == [
            new ProposedRepayment<>("ada", "dan", Money.of(10, "PLN")),
            new ProposedRepayment<>("bob", "dan", Money.of(10, "PLN"))] as Set
    }

    def "eliminates an original obligation cycle into no repayments"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob", "cid"],
                [["ada", "bob", 10], ["bob", "cid", 10], ["cid", "ada", 10]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments().isEmpty()
    }

    def "aggregates parallel obligations and compensates opposite ones"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob"],
                [["ada", "bob", 10], ["ada", "bob", 5], ["bob", "ada", 7]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>("ada", "bob", Money.of(8, "PLN"))]
    }

    def "removes loops and zero obligations before netting"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob"],
                [["ada", "ada", 10], ["ada", "bob", 0], ["ada", "bob", 10]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))]
    }

    def "returns no repayments for settled input"() {
        given:
        Obligations<String> obligations = Obligations.of(["ada", "bob"] as Set, List.of(), PLN)

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments().isEmpty()
        proposal.participants() == ["ada", "bob"] as Set
    }

    def "pairs equal balances deterministically by participant order"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob", "cid", "dan"],
                [["ada", "cid", 10], ["bob", "dan", 10]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments() as Set == [
            new ProposedRepayment<>("ada", "cid", Money.of(10, "PLN")),
            new ProposedRepayment<>("bob", "dan", Money.of(10, "PLN"))] as Set
    }

    def "preserves every participant balance"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob", "cid", "dan"],
                [["ada", "bob", 30], ["bob", "cid", 10], ["cid", "dan", 5], ["dan", "ada", 5]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        balancesOf(obligations) == balancesOfProposal(proposal)
    }

    def "always matches the largest remaining debtor with the largest remaining creditor"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob", "cid", "dan"],
                [["bob", "cid", 90], ["ada", "cid", 5], ["ada", "dan", 95]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        proposal.proposedRepayments() as Set == [
            new ProposedRepayment<>("ada", "cid", Money.of(95, "PLN")),
            new ProposedRepayment<>("bob", "dan", Money.of(90, "PLN")),
            new ProposedRepayment<>("ada", "dan", Money.of(5, "PLN"))] as Set
    }

    def "produces a valid graph within the edge bound"() {
        given:
        Obligations<String> obligations = obligationsOf(
                ["ada", "bob", "cid", "dan", "eva"],
                [["ada", "bob", 30], ["bob", "cid", 10], ["cid", "dan", 5], ["dan", "eva", 5],
                 ["eva", "ada", 5], ["ada", "cid", 7]])

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

        then:
        hasNoLoops(proposal)
        hasNoZeroEdges(proposal)
        hasNoParallelEdges(proposal)
        hasNoCycles(proposal)
        proposal.proposedRepayments().size() <= unbalancedParticipants(obligations) - 1
    }

    def "equal inputs produce an equal repayment set"() {
        given:
        Obligations<String> first = obligationsOf(
                ["ada", "bob", "cid", "dan"],
                [["ada", "cid", 10], ["bob", "dan", 10]])
        Obligations<String> second = obligationsOf(
                ["ada", "bob", "cid", "dan"],
                [["bob", "dan", 10], ["ada", "cid", 10]])

        when:
        ProposedRepayments<String> firstProposal = netting.net(first, order())
        ProposedRepayments<String> secondProposal = netting.net(second, order())

        then:
        firstProposal.proposedRepayments() as Set == secondProposal.proposedRepayments() as Set
    }

    def "respects the smallest unit of #currencyCode"() {
        given:
        CurrencyUnit currency = Monetary.getCurrency(currencyCode)
        Obligations<String> obligations = Obligations.of(
                ["ada", "bob", "cid"] as Set,
                [Obligation.of("ada", "bob", Money.of(new BigDecimal(owed), currencyCode)),
                 Obligation.of("bob", "cid", Money.of(new BigDecimal(owed), currencyCode))],
                currency)

        when:
        ProposedRepayments<String> proposal = netting.net(obligations, order())

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
        Obligations<Integer> obligations = Obligations.of(
                [3, 1, 2] as Set,
                [Obligation.of(3, 1, Money.of(10, "PLN"))],
                PLN)

        when:
        ProposedRepayments<Integer> proposal =
                netting.net(obligations, { a, b -> a <=> b } as ParticipantComparator<Integer>)

        then:
        proposal.proposedRepayments() == [new ProposedRepayment<>(3, 1, Money.of(10, "PLN"))]
    }

    def "rejects a null obligations"() {
        when:
        netting.net(null, order())

        then:
        thrown(NullPointerException)
    }

    def "rejects a null participant order"() {
        given:
        Obligations<String> obligations = obligationsOf(["ada", "bob"], [["ada", "bob", 10]])

        when:
        netting.net(obligations, null)

        then:
        thrown(NullPointerException)
    }

    private static ParticipantComparator<String> order() {
        { a, b -> a <=> b } as ParticipantComparator<String>
    }

    private static Obligations<String> obligationsOf(List<String> participants, List<List> entries) {
        List<Obligation<String>> obligations = entries.collect { entry ->
            Obligation.of(entry[0] as String, entry[1] as String, Money.of(entry[2], "PLN"))
        }
        return Obligations.of(participants as Set, obligations, PLN)
    }

    private static Map<String, Money> balancesOf(Obligations<String> obligations) {
        Map<String, Money> balances = [:].withDefault { Money.zero("PLN") }
        obligations.participants().each { balances[it] = Money.zero("PLN") }
        obligations.obligations().each { obligation ->
            if (obligation.from() == obligation.to() || obligation.amount().isZero()) {
                return
            }
            balances[obligation.from()] = balances[obligation.from()].subtract(obligation.amount())
            balances[obligation.to()] = balances[obligation.to()].add(obligation.amount())
        }
        return balances
    }

    private static Map<String, Money> balancesOfProposal(ProposedRepayments<String> proposal) {
        Map<String, Money> balances = [:].withDefault { Money.zero("PLN") }
        proposal.participants().each { balances[it] = Money.zero("PLN") }
        proposal.proposedRepayments().each { repayment ->
            balances[repayment.debtor()] = balances[repayment.debtor()].subtract(repayment.amount())
            balances[repayment.creditor()] = balances[repayment.creditor()].add(repayment.amount())
        }
        return balances
    }

    private static boolean hasNoLoops(ProposedRepayments<String> proposal) {
        !proposal.proposedRepayments().any { it.debtor() == it.creditor() }
    }

    private static boolean hasNoZeroEdges(ProposedRepayments<String> proposal) {
        !proposal.proposedRepayments().any { it.amount().isZero() || it.amount().isNegative() }
    }

    private static boolean hasNoParallelEdges(ProposedRepayments<String> proposal) {
        List<Set<String>> pairs = proposal.proposedRepayments().collect { [it.debtor(), it.creditor()] as Set }
        pairs.size() == pairs.toSet().size()
    }

    private static boolean hasNoCycles(ProposedRepayments<String> proposal) {
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

    private static int unbalancedParticipants(Obligations<String> obligations) {
        balancesOf(obligations).values().count { !it.isZero() }
    }
}

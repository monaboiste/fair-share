package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.LocalDateTime
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class NettingPropertiesSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 0, 0)
    private static final ParticipantComparator<String> ORDER = { a, b -> a <=> b } as ParticipantComparator<String>

    private final Netting netting = Netting.greedy()

    def "every random obligation set nets deterministically to a valid proposal"() {
        given:
        Random random = new Random(42)

        when:
        List<Map<String, Boolean>> trials = (1..500).collect { netOneRandomGraph(random) }

        then:
        trials.every { it.balancePreserved }

        and:
        trials.every { it.acyclic }

        and:
        trials.every { it.withinEdgeBound }
    }

    private Map<String, Boolean> netOneRandomGraph(Random random) {
        int people = 2 + random.nextInt(5)
        List<String> participants = (0..<people).collect { "p" + it }
        LocalDateTime asOf = BASE.plusDays(random.nextInt(365))

        List<Obligation<String>> obligations = []
        random.nextInt(3 * people + 1).times {
            String from = participants[random.nextInt(people)]
            String to = participants[random.nextInt(people)]
            Money amount = Money.of(1 + random.nextInt(100), "PLN")
            obligations << new Obligation<>(from, to, amount, randomValidity(random, asOf))
        }
        ObligationGraph<String> graph = ObligationGraph.of(participants as Set, obligations, PLN)

        ProposedRepaymentGraph<String> proposal = netting.net(graph, ORDER, asOf)

        Map<String, Money> expected = balances(participants) { balance ->
            obligations.findAll { it.validity().isValidAt(asOf) }.each { obligation ->
                balance[obligation.from()] = balance[obligation.from()].subtract(obligation.amount())
                balance[obligation.to()] = balance[obligation.to()].add(obligation.amount())
            }
        }
        Map<String, Money> actual = balances(participants) { balance ->
            proposal.proposedRepayments().each { repayment ->
                balance[repayment.debtor()] = balance[repayment.debtor()].subtract(repayment.amount())
                balance[repayment.creditor()] = balance[repayment.creditor()].add(repayment.amount())
            }
        }
        int unbalanced = expected.values().count { !it.isZero() }.toInteger()

        return [
            balancePreserved: actual == expected,
            acyclic: acyclic(proposal),
            withinEdgeBound: proposal.proposedRepayments().size() <= Math.max(0, unbalanced - 1)
        ]
    }

    private static Validity randomValidity(Random random, LocalDateTime asOf) {
        switch (random.nextInt(3)) {
            case 0:
                return Validity.always()
            case 1:
                return Validity.between(asOf.minusDays(1), asOf.plusDays(1))
            default:
                return Validity.between(asOf.plusDays(5), asOf.plusDays(10))
        }
    }

    private static Map<String, Money> balances(List<String> participants, Closure<?> accumulate) {
        Map<String, Money> balance = [:]
        participants.each { balance[it] = Money.zero("PLN") }
        accumulate(balance)
        return balance
    }

    private static boolean acyclic(ProposedRepaymentGraph<String> proposal) {
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
}

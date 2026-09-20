package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ObligationGraphSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final CurrencyUnit USD = Monetary.getCurrency("USD")

    def "accepts settled input with no obligations"() {
        when:
        ObligationGraph<String> graph = ObligationGraph.of(["ada", "bob"] as Set, List.of(), PLN)

        then:
        graph.participants() == ["ada", "bob"] as Set
        graph.obligations().isEmpty()
        graph.currency() == PLN
    }

    def "rejects obligations in mixed currencies"() {
        given:
        Obligation<String> local = new Obligation<>("ada", "bob", Money.of(10, "PLN"))
        Obligation<String> foreign = new Obligation<>("ada", "bob", Money.of(10, "USD"))

        when:
        ObligationGraph.of(["ada", "bob"] as Set, [local, foreign], PLN)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects obligations referencing unknown participants"() {
        given:
        Obligation<String> stranger = new Obligation<>("ada", "mallory", Money.of(10, "PLN"))

        when:
        ObligationGraph.of(["ada", "bob"] as Set, [stranger], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("unknown participant")
    }

    def "rejects a null #missing graph input"() {
        when:
        new ObligationGraph<>(participants, obligations, currency)

        then:
        thrown(NullPointerException)

        where:
        missing          | participants          | obligations                                    | currency
        "participants"   | null                  | List.of()                                      | PLN
        "obligations"    | ["ada"] as Set        | null                                           | PLN
        "currency"       | ["ada"] as Set        | List.of()                                      | null
    }

    def "proposed repayment rejects a #description repayment"() {
        when:
        new ProposedRepayment<>(debtor, creditor, amount)

        then:
        thrown(IllegalArgumentException)

        where:
        description   | debtor | creditor | amount
        "self-directed" | "ada"  | "ada"    | Money.of(10, "PLN")
        "zero"          | "ada"  | "bob"    | Money.of(0, "PLN")
        "negative"      | "ada"  | "bob"    | Money.of(-10, "PLN")
    }

    def "proposed repayment graph rejects parallel edges"() {
        given:
        ProposedRepayment<String> first = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> second = new ProposedRepayment<>("ada", "bob", Money.of(5, "PLN"))

        when:
        ProposedRepaymentGraph.of(["ada", "bob"] as Set, [first, second], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("parallel")
    }

    def "proposed repayment graph rejects opposite edges forming a cycle"() {
        given:
        ProposedRepayment<String> outward = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> backward = new ProposedRepayment<>("bob", "ada", Money.of(10, "PLN"))

        when:
        ProposedRepaymentGraph.of(["ada", "bob"] as Set, [outward, backward], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("cycles")
    }

    def "proposed repayment graph rejects a foreign currency"() {
        given:
        ProposedRepayment<String> repayment = new ProposedRepayment<>("ada", "bob", Money.of(10, "USD"))

        when:
        ProposedRepaymentGraph.of(["ada", "bob"] as Set, [repayment], PLN)

        then:
        thrown(IllegalArgumentException)
    }
}

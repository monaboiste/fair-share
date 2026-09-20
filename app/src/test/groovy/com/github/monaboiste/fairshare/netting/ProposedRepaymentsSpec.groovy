package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ProposedRepaymentsSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")

    def "exposes the repayments it was built from"() {
        given:
        ProposedRepayment<String> repayment = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))

        when:
        ProposedRepayments<String> repayments = ProposedRepayments.of(["ada", "bob"] as Set, [repayment], PLN)

        then:
        repayments.proposedRepayments() == [repayment]
        repayments.participants() == ["ada", "bob"] as Set
        repayments.currency() == PLN
    }

    def "proposed repayment rejects a #description repayment"() {
        when:
        new ProposedRepayment<>(debtor, creditor, amount)

        then:
        thrown(IllegalArgumentException)

        where:
        description     | debtor | creditor | amount
        "self-directed" | "ada"  | "ada"    | Money.of(10, "PLN")
        "zero"          | "ada"  | "bob"    | Money.of(0, "PLN")
        "negative"      | "ada"  | "bob"    | Money.of(-10, "PLN")
    }

    def "rejects parallel edges"() {
        given:
        ProposedRepayment<String> first = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> second = new ProposedRepayment<>("ada", "bob", Money.of(5, "PLN"))

        when:
        ProposedRepayments.of(["ada", "bob"] as Set, [first, second], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("parallel")
    }

    def "rejects opposite edges forming a cycle"() {
        given:
        ProposedRepayment<String> outward = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> backward = new ProposedRepayment<>("bob", "ada", Money.of(10, "PLN"))

        when:
        ProposedRepayments.of(["ada", "bob"] as Set, [outward, backward], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("cycles")
    }

    def "rejects longer cycles"() {
        given:
        ProposedRepayment<String> aToB = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> bToC = new ProposedRepayment<>("bob", "cid", Money.of(10, "PLN"))
        ProposedRepayment<String> cToA = new ProposedRepayment<>("cid", "ada", Money.of(10, "PLN"))

        when:
        ProposedRepayments.of(["ada", "bob", "cid"] as Set, [aToB, bToC, cToA], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("cycles")
    }

    def "rejects a foreign currency"() {
        given:
        ProposedRepayment<String> repayment = new ProposedRepayment<>("ada", "bob", Money.of(10, "USD"))

        when:
        ProposedRepayments.of(["ada", "bob"] as Set, [repayment], PLN)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects repayments referencing unknown participants"() {
        given:
        ProposedRepayment<String> stranger = new ProposedRepayment<>("ada", "mallory", Money.of(10, "PLN"))

        when:
        ProposedRepayments.of(["ada", "bob"] as Set, [stranger], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("unknown participant")
    }
}

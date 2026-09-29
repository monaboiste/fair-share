package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ProposedRepaymentsSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")

    def "exposes the repayments it was built from"() {
        given: "a Proposed Repayment of ten zlotys from Ada to Bob"
        ProposedRepayment<String> repayment = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))

        when: "Proposed Repayments are created from it"
        ProposedRepayments<String> repayments = ProposedRepayments.of(["ada", "bob"] as Set, [repayment], PLN)

        then: "the repayment, its Participants and its currency are exposed unchanged"
        repayments.proposedRepayments() == [repayment]
        repayments.participants() == ["ada", "bob"] as Set
        repayments.currency() == PLN
    }

    def "proposed repayment rejects a #description repayment"() {
        when: "a Proposed Repayment is created with invalid details"
        new ProposedRepayment<>(debtor, creditor, amount)

        then: "it is rejected"
        thrown(IllegalArgumentException)

        where:
        description     | debtor | creditor | amount
        "self-directed" | "ada"  | "ada"    | Money.of(10, "PLN")
        "zero"          | "ada"  | "bob"    | Money.of(0, "PLN")
        "negative"      | "ada"  | "bob"    | Money.of(-10, "PLN")
    }

    def "rejects parallel edges"() {
        given: "two Proposed Repayments from Ada to Bob"
        ProposedRepayment<String> first = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> second = new ProposedRepayment<>("ada", "bob", Money.of(5, "PLN"))

        when: "Proposed Repayments are created from both"
        ProposedRepayments.of(["ada", "bob"] as Set, [first, second], PLN)

        then: "they are rejected as parallel"
        IllegalArgumentException error = thrown()
        error.message.contains("parallel")
    }

    def "rejects opposite edges forming a cycle"() {
        given: "Proposed Repayments from Ada to Bob and back again"
        ProposedRepayment<String> outward = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> backward = new ProposedRepayment<>("bob", "ada", Money.of(10, "PLN"))

        when: "Proposed Repayments are created from both"
        ProposedRepayments.of(["ada", "bob"] as Set, [outward, backward], PLN)

        then: "they are rejected as forming a cycle"
        IllegalArgumentException error = thrown()
        error.message.contains("cycles")
    }

    def "rejects longer cycles"() {
        given: "Proposed Repayments running from Ada to Bob to Cid and back to Ada"
        ProposedRepayment<String> aToB = new ProposedRepayment<>("ada", "bob", Money.of(10, "PLN"))
        ProposedRepayment<String> bToC = new ProposedRepayment<>("bob", "cid", Money.of(10, "PLN"))
        ProposedRepayment<String> cToA = new ProposedRepayment<>("cid", "ada", Money.of(10, "PLN"))

        when: "Proposed Repayments are created from them"
        ProposedRepayments.of(["ada", "bob", "cid"] as Set, [aToB, bToC, cToA], PLN)

        then: "they are rejected as forming a cycle"
        IllegalArgumentException error = thrown()
        error.message.contains("cycles")
    }

    def "rejects a foreign currency"() {
        given: "a Proposed Repayment in US dollars"
        ProposedRepayment<String> repayment = new ProposedRepayment<>("ada", "bob", Money.of(10, "USD"))

        when: "Proposed Repayments are created in zlotys"
        ProposedRepayments.of(["ada", "bob"] as Set, [repayment], PLN)

        then: "they are rejected"
        thrown(IllegalArgumentException)
    }

    def "rejects repayments referencing unknown participants"() {
        given: "a Proposed Repayment to someone who is not a Participant"
        ProposedRepayment<String> stranger = new ProposedRepayment<>("ada", "mallory", Money.of(10, "PLN"))

        when: "Proposed Repayments are created for Ada and Bob"
        ProposedRepayments.of(["ada", "bob"] as Set, [stranger], PLN)

        then: "they are rejected for referring to an unknown Participant"
        IllegalArgumentException error = thrown()
        error.message.contains("unknown participant")
    }
}

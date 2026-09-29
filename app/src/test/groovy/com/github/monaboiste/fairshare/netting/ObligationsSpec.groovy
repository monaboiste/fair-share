package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ObligationsSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final CurrencyUnit USD = Monetary.getCurrency("USD")

    def "keeps every participant even with no obligations"() {
        when: "Obligations are created for two Participants without any amounts owed"
        Obligations<String> obligations = Obligations.of(["ada", "bob"] as Set, List.of(), PLN)

        then: "both Participants are kept, with no Obligations, in zlotys"
        obligations.participants() == ["ada", "bob"] as Set
        obligations.obligations().isEmpty()
        obligations.currency() == PLN
    }

    def "exposes the obligations it was built from"() {
        given: "Ada owes Bob ten zlotys"
        Obligation<String> owed = new Obligation<>("ada", "bob", Money.of(10, "PLN"))

        when: "Obligations are created from it"
        Obligations<String> obligations = Obligations.of(["ada", "bob"] as Set, [owed], PLN)

        then: "that Obligation is exposed unchanged"
        obligations.obligations() == [owed]
    }

    def "signed balances include uninvolved participants and sum to zero"() {
        given: "a single Obligation in US dollars among three Participants"
        def obligations = Obligations.of(["ada", "bob", "cal"] as Set,
            [new Obligation<>("ada", "bob", Money.of(10, "USD"))], USD)

        when: "signed Balances are derived"
        def balances = obligations.signedBalances()

        then: "every Participant has a Balance, including the one not involved, and they sum to zero"
        balances == ["ada": Money.of(-10, "USD"), "bob": Money.of(10, "USD"), "cal": Money.zero("USD")]
        balances.values().inject(Money.zero("USD")) { sum, amount -> sum.add(amount) }.isZero()
    }

    def "rejects obligations in mixed currencies"() {
        given: "one Obligation in zlotys and another in US dollars"
        Obligation<String> local = new Obligation<>("ada", "bob", Money.of(10, "PLN"))
        Obligation<String> foreign = new Obligation<>("ada", "bob", Money.of(10, "USD"))

        when: "Obligations are created in zlotys from both"
        Obligations.of(["ada", "bob"] as Set, [local, foreign], PLN)

        then: "they are rejected"
        thrown(IllegalArgumentException)
    }

    def "rejects obligations referencing unknown participants"() {
        given: "an Obligation owed to someone who is not a Participant"
        Obligation<String> stranger = new Obligation<>("ada", "mallory", Money.of(10, "PLN"))

        when: "Obligations are created for Ada and Bob"
        Obligations.of(["ada", "bob"] as Set, [stranger], PLN)

        then: "they are rejected for referring to an unknown Participant"
        IllegalArgumentException error = thrown()
        error.message.contains("unknown participant")
    }

    def "rejects a negative obligation amount"() {
        when: "an Obligation of a negative amount is created"
        new Obligation<>("ada", "bob", Money.of(-10, "PLN"))

        then: "it is rejected"
        thrown(IllegalArgumentException)
    }

    def "rejects a null #missing input"() {
        when: "Obligations are created with missing input"
        Obligations.of(participants, obligations, PLN)

        then: "they are refused"
        thrown(NullPointerException)

        where:
        missing        | participants   | obligations
        "participants" | null           | List.<Obligation<String>>of()
        "obligations"  | ["ada"] as Set | null
    }
}

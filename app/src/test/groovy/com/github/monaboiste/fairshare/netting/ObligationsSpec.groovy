package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ObligationsSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")

    def "keeps every participant even with no obligations"() {
        when:
        Obligations<String> obligations = Obligations.of(["ada", "bob"] as Set, List.of(), PLN)

        then:
        obligations.participants() == ["ada", "bob"] as Set
        obligations.obligations().isEmpty()
        obligations.currency() == PLN
    }

    def "exposes the obligations it was built from"() {
        given:
        Obligation<String> owed = Obligation.of("ada", "bob", Money.of(10, "PLN"))

        when:
        Obligations<String> obligations = Obligations.of(["ada", "bob"] as Set, [owed], PLN)

        then:
        obligations.obligations() == [owed]
    }

    def "rejects obligations in mixed currencies"() {
        given:
        Obligation<String> local = Obligation.of("ada", "bob", Money.of(10, "PLN"))
        Obligation<String> foreign = Obligation.of("ada", "bob", Money.of(10, "USD"))

        when:
        Obligations.of(["ada", "bob"] as Set, [local, foreign], PLN)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects obligations referencing unknown participants"() {
        given:
        Obligation<String> stranger = Obligation.of("ada", "mallory", Money.of(10, "PLN"))

        when:
        Obligations.of(["ada", "bob"] as Set, [stranger], PLN)

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("unknown participant")
    }

    def "rejects a negative obligation amount"() {
        when:
        Obligation.of("ada", "bob", Money.of(-10, "PLN"))

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects a null #missing input"() {
        when:
        Obligations.of(participants, obligations, PLN)

        then:
        thrown(NullPointerException)

        where:
        missing        | participants   | obligations
        "participants" | null           | List.of()
        "obligations"  | ["ada"] as Set | null
    }
}

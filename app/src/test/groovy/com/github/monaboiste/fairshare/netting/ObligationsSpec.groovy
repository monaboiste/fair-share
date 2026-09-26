package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.LocalDateTime
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

    def "always-valid signed balances include uninvolved participants and sum to zero"() {
        given:
        def obligations = Obligations.of(["ada", "bob", "cal"] as Set,
            [Obligation.of("ada", "bob", Money.of(10, "PLN"))], PLN)

        when:
        def balances = obligations.signedBalances()

        then:
        balances == ["ada": Money.of(-10, "PLN"), "bob": Money.of(10, "PLN"), "cal": Money.zero("PLN")]
        balances == obligations.signedBalances(LocalDateTime.of(2000, 1, 1, 0, 0))
        balances == obligations.signedBalances(LocalDateTime.of(2050, 1, 1, 0, 0))
        balances.values().inject(Money.zero("PLN")) { sum, amount -> sum.add(amount) }.isZero()
    }

    def "timeless signed balances reject bounded obligations"() {
        given:
        def bounded = new Obligation<>("ada", "bob", Money.of(10, "PLN"),
            Validity.between(LocalDateTime.of(2025, 1, 1, 0, 0), LocalDateTime.of(2025, 12, 31, 0, 0)))
        def obligations = Obligations.of(["ada", "bob"] as Set,
            [Obligation.of("bob", "ada", Money.of(2, "PLN")), bounded], PLN)

        when:
        obligations.signedBalances()

        then:
        thrown(IllegalStateException)
    }

    def "time-specific signed balances respect obligation validity"() {
        given:
        def start = LocalDateTime.of(2025, 1, 1, 0, 0)
        def end = LocalDateTime.of(2025, 12, 31, 0, 0)
        def obligations = Obligations.of(["ada", "bob"] as Set,
            [new Obligation<>("ada", "bob", Money.of(10, "PLN"), Validity.between(start, end))], PLN)

        expect:
        obligations.signedBalances(start.minusDays(1)) == ["ada": Money.zero("PLN"), "bob": Money.zero("PLN")]
        obligations.signedBalances(start.plusDays(1)) ==
            ["ada": Money.of(-10, "PLN"), "bob": Money.of(10, "PLN")]
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

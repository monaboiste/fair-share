package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class BalancesSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final CurrencyUnit USD = Monetary.getCurrency("USD")

    def "nets parallel, opposite, loop and zero contributions into signed balances that sum to zero"() {
        given: "Obligations with parallel, opposite, self-owed and zero amounts among three Participants"
        Obligations<String> obligations = Obligations.of(
                ["ada", "bob", "cid"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "PLN")),
                 new Obligation<>("ada", "bob", Money.of(5, "PLN")),
                 new Obligation<>("bob", "ada", Money.of(7, "PLN")),
                 new Obligation<>("bob", "bob", Money.of(3, "PLN")),
                 new Obligation<>("cid", "ada", Money.zero("PLN"))],
                PLN)

        when: "Balances are derived"
        Balances<String> balances = Balances.of(obligations)

        then: "each Participant has a signed Balance, debtors and creditors are identified, and all Balances sum to zero"
        balances.amounts() == ["ada": Money.of(-8, "PLN"), "bob": Money.of(8, "PLN"), "cid": Money.zero("PLN")]
        balances.debtors() == ["ada": Money.of(8, "PLN")]
        balances.creditors() == ["bob": Money.of(8, "PLN")]
        balances.amounts().values().inject(Money.zero("PLN")) { sum, amount -> sum.add(amount) }.isZero()
    }

    def "every participant appears, including uninvolved ones in the settlement currency"() {
        given: "a single Obligation in US dollars among three Participants"
        Obligations<String> obligations = Obligations.of(
                ["ada", "bob", "cal"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "USD"))],
                USD)

        when: "Balances are derived"
        Balances<String> balances = Balances.of(obligations)

        then: "every Participant has a Balance in US dollars, including the one not involved"
        balances.amounts() == ["ada": Money.of(-10, "USD"), "bob": Money.of(10, "USD"), "cal": Money.zero("USD")]
    }
}

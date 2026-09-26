package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.quantity.money.Money
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class BalancesSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final CurrencyUnit USD = Monetary.getCurrency("USD")

    def "nets parallel, opposite and loop contributions into signed balances"() {
        given:
        Obligations<String> obligations = Obligations.of(
                ["ada", "bob", "cid"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "PLN")),
                 new Obligation<>("ada", "bob", Money.of(5, "PLN")),
                 new Obligation<>("bob", "ada", Money.of(7, "PLN")),
                 new Obligation<>("bob", "bob", Money.of(3, "PLN"))],
                PLN)

        when:
        Balances<String> balances = Balances.of(obligations)

        then:
        balances.amounts() == ["ada": Money.of(-8, "PLN"), "bob": Money.of(8, "PLN"), "cid": Money.zero("PLN")]
        balances.debtors() == ["ada": Money.of(8, "PLN")]
        balances.creditors() == ["bob": Money.of(8, "PLN")]
    }

    def "every participant appears, including uninvolved ones in the settlement currency"() {
        given:
        Obligations<String> obligations = Obligations.of(
                ["ada", "bob", "cal"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "USD"))],
                USD)

        when:
        Balances<String> balances = Balances.of(obligations)

        then:
        balances.amounts() == ["ada": Money.of(-10, "USD"), "bob": Money.of(10, "USD"), "cal": Money.zero("USD")]
    }

    def "an uninvolved participant keeps a zero balance while others move money"() {
        given:
        Obligations<String> obligations = Obligations.of(
                ["ada", "bob", "cid", "dan"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "USD")),
                 new Obligation<>("ada", "cid", Money.of(4, "USD"))],
                USD)

        when:
        Balances<String> balances = Balances.of(obligations)

        then:
        balances.amounts()["dan"] == Money.zero("USD")
    }

}

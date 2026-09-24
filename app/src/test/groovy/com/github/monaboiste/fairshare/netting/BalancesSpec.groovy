package com.github.monaboiste.fairshare.netting

import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.LocalDateTime
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class BalancesSpec extends Specification {

    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final CurrencyUnit USD = Monetary.getCurrency("USD")
    private static final LocalDateTime START = LocalDateTime.of(2025, 1, 1, 0, 0)
    private static final LocalDateTime END = LocalDateTime.of(2025, 12, 31, 0, 0)
    private static final LocalDateTime BEFORE = LocalDateTime.of(2024, 6, 1, 0, 0)
    private static final LocalDateTime DURING = LocalDateTime.of(2025, 6, 1, 0, 0)

    def "nets parallel, opposite and loop contributions into signed balances"() {
        given:
        Obligations<String> graph = Obligations.of(
                ["ada", "bob", "cid"] as Set,
                [always("ada", "bob", 10), always("ada", "bob", 5), always("bob", "ada", 7), always("bob", "bob", 3)],
                PLN)

        when:
        Balances<String> balances = Balances.of(graph, DURING)

        then:
        balances.amounts() == ["ada": Money.of(-8, "PLN"), "bob": Money.of(8, "PLN"), "cid": Money.zero("PLN")]
        balances.debtors() == ["ada": Money.of(8, "PLN")]
        balances.creditors() == ["bob": Money.of(8, "PLN")]
    }

    def "an obligation contributes only while it is valid - non-PLN currency"() {
        given:
        Obligations<String> graph = Obligations.of(
                ["ada", "bob"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "USD"), Validity.between(START, END))],
                USD)

        expect: "outside the window every balance is a zero in the settlement currency, never a PLN zero"
        Balances.of(graph, BEFORE).amounts() == ["ada": Money.zero("USD"), "bob": Money.zero("USD")]

        and: "inside the window the obligation moves money"
        Balances.of(graph, DURING).amounts() == ["ada": Money.of(-10, "USD"), "bob": Money.of(10, "USD")]
    }

    def "an obligation outside its validity is explained as a zero contribution in the settlement currency"() {
        given:
        Obligations<String> graph = Obligations.of(
                ["ada", "bob"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "USD"), Validity.between(START, END))],
                USD)

        when:
        def breakdown = Balances.of(graph, BEFORE).breakdown("ada")

        then:
        breakdown.total() == Money.zero("USD")
        breakdown.children()*.name() == ["ada->bob"]
        breakdown.children()*.total() == [Money.zero("USD")]
    }

    def "breakdown explains a balance as its contributing obligations"() {
        given:
        Obligations<String> graph = Obligations.of(
                ["ada", "bob"] as Set,
                [always("ada", "bob", 10), always("ada", "bob", 5), always("bob", "ada", 7)],
                PLN)

        when:
        def breakdown = Balances.of(graph, DURING).breakdown("ada")

        then:
        breakdown.total() == Money.of(-8, "PLN")
        breakdown.children().size() == 3
    }

    def "simulate recomputes balances at each time"() {
        given:
        Obligations<String> graph = Obligations.of(
                ["ada", "bob"] as Set,
                [new Obligation<>("ada", "bob", Money.of(10, "USD"), Validity.between(START, END))],
                USD)

        when:
        Map<LocalDateTime, Balances<String>> simulated = Balances.of(graph, DURING).simulate([BEFORE, DURING])

        then:
        simulated[BEFORE].amounts() == ["ada": Money.zero("USD"), "bob": Money.zero("USD")]
        simulated[DURING].amounts() == ["ada": Money.of(-10, "USD"), "bob": Money.of(10, "USD")]
    }

    private static Obligation<String> always(String from, String to, int amount) {
        Obligation.of(from, to, Money.of(amount, "PLN"))
    }
}

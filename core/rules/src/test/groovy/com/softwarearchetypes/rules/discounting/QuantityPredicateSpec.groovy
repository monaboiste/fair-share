package com.softwarearchetypes.rules.discounting

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import com.softwarearchetypes.quantity.money.Money
import com.softwarearchetypes.rules.discounting.offer.OfferItem
import com.softwarearchetypes.rules.discounting.offer.modifiers.functors.predicates.QuantityPredicate
import spock.lang.Specification

class QuantityPredicateSpec extends Specification {

    def "matches quantities above the configured threshold"() {
        given:
        def predicate = new QuantityPredicate(Quantity.of(10, Unit.pieces()))
        def item = new OfferItem(
                UUID.randomUUID(),
                Quantity.of(amount, Unit.pieces()),
                Money.zero("PLN")
        )

        expect:
        predicate.test(item) == matches

        where:
        amount || matches
        9      || false
        10     || false
        11     || true
    }

    def "rejects comparison with an incompatible unit"() {
        given:
        def predicate = new QuantityPredicate(Quantity.of(10, Unit.pieces()))
        def item = new OfferItem(
                UUID.randomUUID(),
                Quantity.of(11, Unit.kilograms()),
                Money.zero("PLN")
        )

        when:
        predicate.test(item)

        then:
        thrown(IllegalArgumentException)
    }
}

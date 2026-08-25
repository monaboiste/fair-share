package com.softwarearchetypes.common

import spock.lang.Specification

class PairSpec extends Specification {

    def "creates a pair with both values"() {
        when:
        def pair = new Pair<>("first", "second")

        then:
        pair.first() == "first"
        pair.second() == "second"
    }

    def "creates a pair with null values"() {
        when:
        def pair = new Pair<>(null, null)

        then:
        pair.first() == null
        pair.second() == null
    }

    def "creates a pair with integer values"() {
        when:
        def pair = new Pair<>(42, 100)

        then:
        pair.first() == 42
        pair.second() == 100
    }

    def "pairs with the same values are equal"() {
        given:
        def firstPair = new Pair<>("A", "B")
        def secondPair = new Pair<>("A", "B")

        expect:
        firstPair == secondPair
        firstPair.hashCode() == secondPair.hashCode()
    }

    def "pairs with different values are not equal"() {
        expect:
        new Pair<>("A", "B") != new Pair<>(first, second)

        where:
        first | second
        "C"   | "B"
        "A"   | "C"
        "C"   | "D"
    }

    def "has the record string representation"() {
        expect:
        new Pair<>("first", "second").toString() == "Pair[first=first, second=second]"
    }

    def "creates a pair with the same value for both elements"() {
        when:
        def pair = new Pair<>("same", "same")

        then:
        pair.first() == "same"
        pair.second() == "same"
        pair.first() == pair.second()
    }
}

package com.softwarearchetypes.common

import spock.lang.Specification

class CollectionTransformationsSpec extends Specification {

    def "creates key-value map from alternating arguments"() {
        when:
        def result = CollectionTransformations.keyValueMapFrom([
                "first", "one",
                "second", "two"
        ] as String[])

        then:
        result == [first: "one", second: "two"]
    }

    def "later value replaces an earlier value for the same key"() {
        expect:
        CollectionTransformations.keyValueMapFrom([
                "key", "first",
                "key", "second"
        ] as String[]) == [key: "second"]
    }

    def "creates an empty map from #description arguments"() {
        expect:
        CollectionTransformations.keyValueMapFrom(parameters).isEmpty()

        where:
        description | parameters
        "null"      | null
        "empty"     | [] as String[]
    }

    def "map created from null arguments remains mutable"() {
        given:
        def result = CollectionTransformations.keyValueMapFrom(null)

        when:
        result.put("key", "value")

        then:
        result == [key: "value"]
    }

    def "rejects an odd number of arguments"() {
        when:
        CollectionTransformations.keyValueMapFrom(["key", "value", "orphan"] as String[])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("number of arguments must be even")
    }

    def "rejects a blank key at pair index #pairIndex"() {
        when:
        CollectionTransformations.keyValueMapFrom(parameters as String[])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message.contains("idx: ${pairIndex * 2}")

        where:
        pairIndex | parameters
        0         | ["", "value"]
        1         | ["valid", "value", "   ", "other"]
    }

    def "subtracts values without changing either input"() {
        given:
        def minuend = ["first", "second", "third"] as Set
        def subtrahend = ["second", "missing"] as Set

        when:
        def result = CollectionTransformations.subtract(minuend, subtrahend)

        then:
        result == ["first", "third"] as Set
        minuend == ["first", "second", "third"] as Set
        subtrahend == ["second", "missing"] as Set
    }
}

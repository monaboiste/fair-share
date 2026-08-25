package com.softwarearchetypes.common

import spock.lang.Specification

class ResultSpec extends Specification {

    def "success exposes only its success value"() {
        given:
        def result = Result.success("value")

        expect:
        result.success()
        !result.failure()
        result.getSuccess() == "value"

        when:
        result.getFailure()

        then:
        thrown(IllegalStateException)
    }

    def "failure exposes only its failure value"() {
        given:
        def result = Result.failure("error")

        expect:
        result.failure()
        !result.success()
        result.getFailure() == "error"

        when:
        result.getSuccess()

        then:
        thrown(IllegalStateException)
    }

    def "ifSuccessOrElse maps the active side"() {
        expect:
        result.ifSuccessOrElse({ "SUCCESS-${it}" }, { "FAILURE-${it}" }) == expected

        where:
        result                  | expected
        Result.success("value") | "SUCCESS-value"
        Result.failure("error") | "FAILURE-error"
    }

    def "biMap maps the active side"() {
        expect:
        mapped.success() == success
        success ? mapped.getSuccess() == expected : mapped.getFailure() == expected

        where:
        mapped                                           | success | expected
        Result.success(1).biMap({ "${it * 2}" }, { "" }) | true    | "2"
        Result.failure(1).biMap({ "${it * 2}" }, { "" }) | false   | ""
    }

    def "peek invokes only the active consumer and returns the result"() {
        given:
        def successes = []
        def failures = []

        when:
        def returned = result.peek({ successes << it }, { failures << it })

        then:
        returned.is(result)
        successes == expectedSuccesses
        failures == expectedFailures

        where:
        result             | expectedSuccesses | expectedFailures
        Result.success(10) | [10]              | []
        Result.failure(20) | []                | [20]
    }

    def "side-specific peek invokes only the active consumer and returns the result"() {
        given:
        def successes = []
        def failures = []

        when:
        def returned = result
                .peekSuccess({ successes << it })
                .peekFailure({ failures << it })

        then:
        returned.is(result)
        successes == expectedSuccesses
        failures == expectedFailures

        where:
        result             | expectedSuccesses | expectedFailures
        Result.success(10) | [10]              | []
        Result.failure(20) | []                | [20]
    }

    def "combine maps two successes"() {
        expect:
        Result.success(10)
                .combine(Result.success(20), { first, second -> first - second }, { first, second -> first + second })
                .getSuccess() == 30
    }

    def "combine maps two failures"() {
        expect:
        Result.failure(10)
                .combine(Result.failure(3), { first, second -> first - second }, { first, second -> first + second })
                .getFailure() == 7
    }

    def "combine passes null for the successful side of a mixed pair"() {
        given:
        def failureCombiner = { first, second -> (first ?: 0) - (second ?: 0) }
        def successCombiner = { first, second -> first + second }

        expect:
        Result.success(10)
                .combine(Result.failure(3), failureCombiner, successCombiner)
                .getFailure() == -3
        Result.failure(3)
                .combine(Result.success(10), failureCombiner, successCombiner)
                .getFailure() == 3
    }

    def "map transforms a success"() {
        when:
        def result = Result.success(10).map({ "Value: ${it * 2}" })

        then:
        result.success()
        result.getSuccess() == "Value: 20"
    }

    def "map preserves a failure"() {
        when:
        def result = Result.failure("Error occurred").map({ "Value: ${it * 2}" })

        then:
        result.failure()
        result.getFailure() == "Error occurred"
    }

    def "mapFailure transforms a failure"() {
        when:
        def result = Result.failure(404).mapFailure({ "Error ${it}: Not Found" })

        then:
        result.failure()
        result.getFailure() == "Error 404: Not Found"
    }

    def "mapFailure preserves a success"() {
        when:
        def result = Result.success(42).mapFailure({ "Error ${it}" })

        then:
        result.success()
        result.getSuccess() == 42
    }

    def "flatMap transforms a success into the mapped result"() {
        expect:
        result.success() == success
        success ? result.getSuccess() == expected : result.getFailure() == expected

        where:
        result                                                 | success | expected
        Result.success(5).flatMap({ Result.success(it * 2) })  | true    | 10
        Result.success(5).flatMap({ Result.failure("error") }) | false   | "error"
    }

    def "flatMap preserves a failure"() {
        when:
        def result = Result.failure("Initial error").flatMap({ Result.success(it * 2) })

        then:
        result.failure()
        result.getFailure() == "Initial error"
    }

    def "fold maps the active side"() {
        expect:
        folded == expected

        where:
        folded                                               | expected
        Result.success(10).fold({ -1 }, { it * 3 })          | 30
        Result.failure("Error").fold({ it.length() }, { 0 }) | 5
    }

    def "mapping operations reject null functions"() {
        when:
        operation()

        then:
        thrown(IllegalArgumentException)

        where:
        operation << [
                { Result.success(10).map(null) },
                { Result.failure("error").mapFailure(null) },
                { Result.success(10).flatMap(null) },
                { Result.success(10).fold(null, { it * 2 }) },
                { Result.success(10).fold({ -1 }, null) },
                { Result.success(10).biMap(null, { "" }) },
                { Result.success(10).biMap({ "" }, null) },
                { Result.success(10).ifSuccessOrElse(null, { "" }) },
                { Result.success(10).ifSuccessOrElse({ "" }, null) }
        ]
    }

    def "peek operations reject null consumers"() {
        when:
        operation()

        then:
        thrown(IllegalArgumentException)

        where:
        operation << [
                { Result.success(10).peek(null, {}) },
                { Result.success(10).peek({}, null) },
                { Result.success(10).peekSuccess(null) },
                { Result.failure("error").peekFailure(null) }
        ]
    }

    def "combine rejects null arguments"() {
        when:
        operation()

        then:
        thrown(IllegalArgumentException)

        where:
        operation << [
                { Result.success(10).combine(null, { "" }, { 0 }) },
                { Result.success(10).combine(Result.success(20), null, { first, second -> first + second }) },
                { Result.success(10).combine(Result.success(20), { "" }, null) }
        ]
    }

    def "creates an empty list composite"() {
        when:
        def result = Result.composite().toResult()

        then:
        result.success()
        result.getSuccess().isEmpty()
    }

    def "creates an empty set composite"() {
        when:
        def result = Result.compositeSet().toResult()

        then:
        result.success()
        result.getSuccess().isEmpty()
    }

    def "accumulates successes into a list"() {
        when:
        def result = Result.composite()
                .accumulate(Result.success(1))
                .accumulate(Result.success(2))
                .accumulate(Result.success(3))
                .toResult()

        then:
        result.success()
        result.getSuccess() == [1, 2, 3]
    }

    def "accumulates successes into a set and removes duplicates"() {
        when:
        def result = Result.compositeSet()
                .accumulate(Result.success(1))
                .accumulate(Result.success(2))
                .accumulate(Result.success(1))
                .accumulate(Result.success(3))
                .toResult()

        then:
        result.success()
        result.getSuccess() == [1, 2, 3] as Set
    }

    def "list composite stops at and retains its first failure"() {
        when:
        def result = Result.composite()
                .accumulate(Result.success(1))
                .accumulate(Result.failure("First error"))
                .accumulate(Result.success(2))
                .toResult()

        then:
        result.failure()
        result.getFailure() == "First error"
    }

    def "set composite stops at and retains its first failure"() {
        when:
        def result = Result.compositeSet()
                .accumulate(Result.success(1))
                .accumulate(Result.failure("First error"))
                .accumulate(Result.success(2))
                .toResult()

        then:
        result.failure()
        result.getFailure() == "First error"
    }

    def "composites reject null results"() {
        when:
        composite.accumulate(null)

        then:
        thrown(IllegalArgumentException)

        where:
        composite << [Result.composite(), Result.compositeSet()]
    }

    def "list composite reports its state"() {
        expect:
        composite.success() == success
        composite.failure() != success

        where:
        composite                                              | success
        Result.composite().accumulate(Result.success(1))       | true
        Result.composite().accumulate(Result.failure("Error")) | false
    }

    def "set composite reports its state"() {
        expect:
        composite.success() == success
        composite.failure() != success

        where:
        composite                                                 | success
        Result.compositeSet().accumulate(Result.success(1))       | true
        Result.compositeSet().accumulate(Result.failure("Error")) | false
    }
}

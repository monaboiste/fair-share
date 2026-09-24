package com.github.monaboiste.fairshare.common.queries

import com.github.monaboiste.fairshare.common.Result
import spock.lang.Specification

class QueryDispatcherSpec extends Specification {
    static sealed interface Reads permits First, Second {}
    static record First() implements Reads, Query<String, String> {}
    static record Second() implements Reads, Query<String, String> {}

    def "registered queries return typed successes and failures"() {
        given:
        def dispatcher = RegisteredQueryDispatcher.builder()
            .register(First, { Result.success("found") })
            .register(Second, { Result.failure("missing") })
            .requireHandlersFor(Reads).build()

        when:
        def found = dispatcher.dispatch(new First())
        def missing = dispatcher.dispatch(new Second())

        then:
        found.getSuccess() == "found"
        missing.getFailure() == "missing"
    }

    def "duplicate, incomplete and unknown query handlers fail"() {
        when:
        RegisteredQueryDispatcher.builder().register(First, { Result.success("one") })
            .register(First, { Result.success("two") })

        then:
        thrown(IllegalArgumentException)

        when:
        RegisteredQueryDispatcher.builder().register(First, { Result.success("found") })
            .requireHandlersFor(Reads).build()

        then:
        thrown(IllegalStateException)

        when:
        RegisteredQueryDispatcher.builder().build().dispatch(new Second())

        then:
        thrown(IllegalArgumentException)
    }
}

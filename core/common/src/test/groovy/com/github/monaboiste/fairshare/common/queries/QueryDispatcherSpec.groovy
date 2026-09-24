package com.github.monaboiste.fairshare.common.queries

import spock.lang.Specification

class QueryDispatcherSpec extends Specification {
    static sealed interface Reads permits First, Second {}
    static record First() implements Reads, Query<String> {}
    static record Second() implements Reads, Query<String> {}

    def "registered query returns its result"() {
        given:
        def dispatcher = RegisteredQueryDispatcher.builder().register(First, { "found" })
            .register(Second, { "other" }).requireHandlersFor(Reads).build()

        expect:
        dispatcher.dispatch(new First()) == "found"
    }

    def "duplicate, missing and unknown handlers fail"() {
        when:
        RegisteredQueryDispatcher.builder().register(First, { "one" }).register(First, { "two" })

        then:
        thrown(IllegalArgumentException)

        when:
        RegisteredQueryDispatcher.builder().requireHandlersFor(Reads).build()

        then:
        thrown(IllegalStateException)

        when:
        RegisteredQueryDispatcher.builder().build().dispatch(new Second())

        then:
        thrown(IllegalArgumentException)
    }
}

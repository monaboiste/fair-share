package com.github.monaboiste.fairshare.common.commands

import com.github.monaboiste.fairshare.common.Result
import spock.lang.Specification

class CommandDispatcherSpec extends Specification {
    static record Rejected() implements CommandFailure {}
    static sealed interface Tasks permits First, Second {}
    static record First() implements Tasks, Command<Rejected, String> {}
    static record Second() implements Tasks, Command<Rejected, String> {}

    def "registered command returns typed result"() {
        given:
        def dispatcher = RegisteredCommandDispatcher.builder()
            .register(First, { Result.success("done") }).build()

        expect:
        dispatcher.dispatch(new First()).getSuccess() == "done"
    }

    def "registration rejects duplicates and incomplete sealed families"() {
        when:
        RegisteredCommandDispatcher.builder().register(First, { Result.success("one") })
            .register(First, { Result.success("two") })

        then:
        thrown(IllegalArgumentException)

        when:
        RegisteredCommandDispatcher.builder().register(First, { Result.success("one") })
            .requireHandlersFor(Tasks).build()

        then:
        thrown(IllegalStateException)
    }

    def "unknown commands fail at dispatch"() {
        when:
        RegisteredCommandDispatcher.builder().build().dispatch(new First())

        then:
        thrown(IllegalArgumentException)
    }

    def "interceptors wrap the handler in registration order"() {
        given:
        List<String> visited = []
        def first = new CommandInterceptor() {
            def <F extends CommandFailure, S> Result<F, S> intercept(Command<F, S> command, CommandInterceptor.Proceed next) {
                visited.add("first before")
                def result = next.handle(command)
                visited.add("first after")
                result
            }
        }
        def second = new CommandInterceptor() {
            def <F extends CommandFailure, S> Result<F, S> intercept(Command<F, S> command, CommandInterceptor.Proceed next) {
                visited.add("second before")
                def result = next.handle(command)
                visited.add("second after")
                result
            }
        }
        def dispatcher = RegisteredCommandDispatcher.builder()
            .register(First, { visited.add("handler"); Result.success("done") })
            .intercept(first).intercept(second).build()

        when:
        dispatcher.dispatch(new First())

        then:
        visited == ["first before", "second before", "handler", "second after", "first after"]
    }
}

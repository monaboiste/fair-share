package com.github.monaboiste.fairshare.common.commands

import spock.lang.Specification

class CommandDispatcherSpec extends Specification {
    private static record Greeting(String name) implements Command<String> {}
    private static record CountLetters(String value) implements Command<Integer> {}

    def "registered handlers return results matching each command"() {
        given:
        CommandDispatcher bus = new RegisteredCommandDispatcher([
            new CommandHandler<Greeting, String>() {
                Class<Greeting> commandType() { Greeting }
                String handle(Greeting command) { "Hello ${command.name()}" }
            },
            new CommandHandler<CountLetters, Integer>() {
                Class<CountLetters> commandType() { CountLetters }
                Integer handle(CountLetters command) { command.value().length() }
            }
        ])

        expect:
        bus.dispatch(new Greeting("Ada")) == "Hello Ada"
        bus.dispatch(new CountLetters("Ada")) == 3
    }

    def "duplicate command registrations fail instead of silently replacing a handler"() {
        given:
        def handler = new CommandHandler<Greeting, String>() {
            Class<Greeting> commandType() { Greeting }
            String handle(Greeting command) { command.name() }
        }

        when:
        new RegisteredCommandDispatcher([handler, handler])

        then:
        thrown(IllegalArgumentException)
    }
}
